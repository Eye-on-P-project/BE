package ac.jwooo.eye_on.domain.organization.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringEventLogRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.OrganizationRiskCountProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.OrganizationUserRiskCountProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.TimeBucketRiskCountProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.TimeBucketSessionCountProjection;
import ac.jwooo.eye_on.domain.organization.application.dto.request.RiskStatsGranularity;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskStatsResponse;
import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationDailyRiskStat;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationDailyRiskStatRepository;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationTopRiskUserProjection;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationUserDailyRiskStatRepository;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetOrganizationRiskStatsUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int HOURLY_MAX_DAYS = 31;

    private final OrganizationAccessService organizationAccessService;
    private final OrganizationDailyRiskStatRepository organizationDailyRiskStatRepository;
    private final OrganizationUserDailyRiskStatRepository organizationUserDailyRiskStatRepository;
    private final MonitoringSessionRepository monitoringSessionRepository;
    private final MonitoringEventLogRepository monitoringEventLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public OrganizationRiskStatsResponse execute(
            Long requesterUserId,
            Long organizationId,
            String granularityValue,
            LocalDate from,
            LocalDate to
    ) {
        organizationAccessService.validateAdminAccess(requesterUserId, organizationId);

        RiskStatsGranularity granularity = RiskStatsGranularity.from(granularityValue);
        validateInput(granularity, from, to);

        if (granularity == RiskStatsGranularity.HOUR) {
            return buildHourlyResponse(organizationId, from, to);
        }
        return buildDailyBasedResponse(organizationId, granularity, from, to);
    }

    private OrganizationRiskStatsResponse buildHourlyResponse(Long organizationId, LocalDate from, LocalDate to) {
        long daySpan = ChronoUnit.DAYS.between(from, to) + 1;
        if (daySpan > HOURLY_MAX_DAYS) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "시간 단위 조회는 최대 31일까지만 지원합니다.");
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEndExclusive = to.plusDays(1).atStartOfDay();
        LocalDateTime nowExclusive = nowExclusive();
        if (rangeEndExclusive.isAfter(nowExclusive)) {
            rangeEndExclusive = nowExclusive;
        }

        Map<LocalDateTime, long[]> sessionCountsByHour = new HashMap<>();
        for (TimeBucketSessionCountProjection projection : monitoringSessionRepository
                .findHourlySessionCountsByOrganizationAndRange(organizationId, rangeStart, rangeEndExclusive)) {
            LocalDateTime bucketStart = toHourStart(projection.getYear(), projection.getMonth(), projection.getDay(), projection.getHour());
            sessionCountsByHour.put(bucketStart, new long[] {nullSafe(projection.getSessionCount())});
        }

        Map<LocalDateTime, long[]> riskCountsByHour = new HashMap<>();
        for (TimeBucketRiskCountProjection projection : monitoringEventLogRepository
                .findHourlyRiskCountsByOrganizationAndRange(organizationId, rangeStart, rangeEndExclusive)) {
            LocalDateTime bucketStart = toHourStart(projection.getYear(), projection.getMonth(), projection.getDay(), projection.getHour());
            riskCountsByHour.put(bucketStart, new long[] {
                    nullSafe(projection.getDrowsyCount()),
                    nullSafe(projection.getSleepCount()),
                    nullSafe(projection.getTotalRiskCount())
            });
        }

        List<OrganizationRiskStatsResponse.RiskStatsBucket> series = new ArrayList<>();
        LocalDateTime cursor = rangeStart;
        while (cursor.isBefore(rangeEndExclusive)) {
            LocalDateTime bucketEndExclusive = cursor.plusHours(1);
            long sessionCount = sessionCountsByHour.getOrDefault(cursor, new long[] {0L})[0];
            long[] risk = riskCountsByHour.getOrDefault(cursor, new long[] {0L, 0L, 0L});
            series.add(new OrganizationRiskStatsResponse.RiskStatsBucket(
                    cursor,
                    bucketEndExclusive.minusSeconds(1),
                    sessionCount,
                    risk[0],
                    risk[1],
                    risk[2]
            ));
            cursor = bucketEndExclusive;
        }

        List<OrganizationRiskStatsResponse.RiskTopMember> top5 = buildTopRiskMembersFromRaw(organizationId, rangeStart, rangeEndExclusive);
        return new OrganizationRiskStatsResponse(RiskStatsGranularity.HOUR, from, to, series, top5);
    }

    private OrganizationRiskStatsResponse buildDailyBasedResponse(
            Long organizationId,
            RiskStatsGranularity granularity,
            LocalDate from,
            LocalDate to
    ) {
        Map<LocalDate, DailyCount> dailyMap = new HashMap<>();
        for (OrganizationDailyRiskStat stat : organizationDailyRiskStatRepository
                .findAllByOrganizationIdAndStatDateBetweenAndDeletedAtIsNullOrderByStatDateAsc(organizationId, from, to)) {
            dailyMap.put(stat.getStatDate(), DailyCount.from(stat));
        }

        LocalDate today = LocalDate.now(KST);
        if (!today.isBefore(from) && !today.isAfter(to)) {
            dailyMap.put(today, fetchLiveDailyCount(organizationId, today));
        }

        List<DateBucket> buckets = buildDateBuckets(from, to, granularity);
        List<OrganizationRiskStatsResponse.RiskStatsBucket> series = buckets.stream()
                .map(bucket -> toSeriesBucket(bucket, dailyMap))
                .toList();

        List<OrganizationRiskStatsResponse.RiskTopMember> top5 = buildTopRiskMembersWithDailyAggregation(
                organizationId,
                from,
                to
        );

        return new OrganizationRiskStatsResponse(granularity, from, to, series, top5);
    }

    private DailyCount fetchLiveDailyCount(Long organizationId, LocalDate date) {
        LocalDateTime rangeStart = date.atStartOfDay();
        LocalDateTime rangeEndExclusive = nowExclusive();

        long sessionCount = nullSafe(
                monitoringSessionRepository.findSingleOrganizationSessionCountByRange(
                        organizationId,
                        rangeStart,
                        rangeEndExclusive
                ).getSessionCount()
        );

        OrganizationRiskCountProjection riskProjection = monitoringEventLogRepository.findSingleOrganizationRiskCountByRange(
                organizationId,
                rangeStart,
                rangeEndExclusive
        );
        long drowsyCount = nullSafe(riskProjection.getDrowsyCount());
        long sleepCount = nullSafe(riskProjection.getSleepCount());
        long totalRiskCount = nullSafe(riskProjection.getTotalRiskCount());
        return new DailyCount(sessionCount, drowsyCount, sleepCount, totalRiskCount);
    }

    private List<OrganizationRiskStatsResponse.RiskTopMember> buildTopRiskMembersWithDailyAggregation(
            Long organizationId,
            LocalDate from,
            LocalDate to
    ) {
        Map<Long, TopMemberAccumulator> accumulatorMap = new HashMap<>();

        LocalDate today = LocalDate.now(KST);
        LocalDate aggregatedTo = to.isBefore(today) ? to : today.minusDays(1);
        if (!aggregatedTo.isBefore(from)) {
            for (OrganizationTopRiskUserProjection projection : organizationUserDailyRiskStatRepository
                    .findUserRiskTotalsByOrganizationAndDateRange(organizationId, from, aggregatedTo)) {
                accumulatorMap.put(
                        projection.getUserId(),
                        new TopMemberAccumulator(projection.getName(), nullSafe(projection.getTotalRiskCount()))
                );
            }
        }

        if (!today.isBefore(from) && !today.isAfter(to)) {
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime nowExclusive = nowExclusive();

            for (OrganizationUserRiskCountProjection projection : monitoringEventLogRepository
                    .findUserRiskCountsByOrganizationAndRange(organizationId, todayStart, nowExclusive)) {
                long userId = projection.getUserId();
                TopMemberAccumulator accumulator = accumulatorMap.computeIfAbsent(userId, ignored -> new TopMemberAccumulator(null, 0L));
                accumulator.totalRiskCount += nullSafe(projection.getTotalRiskCount());
            }
        }

        fillMissingNames(accumulatorMap);

        return accumulatorMap.entrySet().stream()
                .filter(entry -> entry.getValue().totalRiskCount > 0L)
                .sorted(Comparator
                        .comparingLong((Map.Entry<Long, TopMemberAccumulator> entry) -> entry.getValue().totalRiskCount).reversed()
                        .thenComparingLong(Map.Entry::getKey))
                .limit(5)
                .map(entry -> new OrganizationRiskStatsResponse.RiskTopMember(
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().totalRiskCount
                ))
                .toList();
    }

    private List<OrganizationRiskStatsResponse.RiskTopMember> buildTopRiskMembersFromRaw(
            Long organizationId,
            LocalDateTime rangeStart,
            LocalDateTime rangeEndExclusive
    ) {
        Map<Long, TopMemberAccumulator> accumulatorMap = new HashMap<>();
        for (OrganizationUserRiskCountProjection projection : monitoringEventLogRepository
                .findUserRiskCountsByOrganizationAndRange(organizationId, rangeStart, rangeEndExclusive)) {
            accumulatorMap.put(
                    projection.getUserId(),
                    new TopMemberAccumulator(null, nullSafe(projection.getTotalRiskCount()))
            );
        }
        fillMissingNames(accumulatorMap);

        return accumulatorMap.entrySet().stream()
                .filter(entry -> entry.getValue().totalRiskCount > 0L)
                .sorted(Comparator
                        .comparingLong((Map.Entry<Long, TopMemberAccumulator> entry) -> entry.getValue().totalRiskCount).reversed()
                        .thenComparingLong(Map.Entry::getKey))
                .limit(5)
                .map(entry -> new OrganizationRiskStatsResponse.RiskTopMember(
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().totalRiskCount
                ))
                .toList();
    }

    private void fillMissingNames(Map<Long, TopMemberAccumulator> accumulatorMap) {
        List<Long> missingNameUserIds = accumulatorMap.entrySet().stream()
                .filter(entry -> entry.getValue().name == null)
                .map(Map.Entry::getKey)
                .toList();
        if (missingNameUserIds.isEmpty()) {
            return;
        }

        Map<Long, String> namesById = new HashMap<>();
        for (User user : userRepository.findAllByIdInAndDeletedAtIsNull(missingNameUserIds)) {
            namesById.put(user.getId(), user.getName());
        }

        for (Long userId : missingNameUserIds) {
            TopMemberAccumulator accumulator = accumulatorMap.get(userId);
            accumulator.name = namesById.getOrDefault(userId, "");
        }
    }

    private OrganizationRiskStatsResponse.RiskStatsBucket toSeriesBucket(DateBucket bucket, Map<LocalDate, DailyCount> dailyMap) {
        long sessionCount = 0L;
        long drowsyCount = 0L;
        long sleepCount = 0L;
        long totalRiskCount = 0L;

        LocalDate cursor = bucket.startDate();
        while (!cursor.isAfter(bucket.endDate())) {
            DailyCount dailyCount = dailyMap.get(cursor);
            if (dailyCount != null) {
                sessionCount += dailyCount.sessionCount();
                drowsyCount += dailyCount.drowsyCount();
                sleepCount += dailyCount.sleepCount();
                totalRiskCount += dailyCount.totalRiskCount();
            }
            cursor = cursor.plusDays(1);
        }

        return new OrganizationRiskStatsResponse.RiskStatsBucket(
                bucket.startDate().atStartOfDay(),
                bucket.endDate().atTime(23, 59, 59),
                sessionCount,
                drowsyCount,
                sleepCount,
                totalRiskCount
        );
    }

    private List<DateBucket> buildDateBuckets(LocalDate from, LocalDate to, RiskStatsGranularity granularity) {
        List<DateBucket> buckets = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            LocalDate bucketEnd = switch (granularity) {
                case DAY -> cursor;
                case WEEK -> min(cursor.plusDays(6), to);
                case MONTH -> min(cursor.plusMonths(1).minusDays(1), to);
                case YEAR -> min(cursor.plusYears(1).minusDays(1), to);
                case HOUR -> cursor;
            };

            buckets.add(new DateBucket(cursor, bucketEnd));
            cursor = bucketEnd.plusDays(1);
        }
        return buckets;
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime nowExclusive() {
        return LocalDateTime.now(KST).withNano(0).plusSeconds(1);
    }

    private LocalDateTime toHourStart(Integer year, Integer month, Integer day, Integer hour) {
        return LocalDateTime.of(
                safeInt(year),
                safeInt(month),
                safeInt(day),
                safeInt(hour),
                0,
                0
        );
    }

    private int safeInt(Integer value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "시간 버킷 집계 결과가 올바르지 않습니다.");
        }
        return value;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private void validateInput(RiskStatsGranularity granularity, LocalDate from, LocalDate to) {
        if (granularity == null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "granularity는 HOUR, DAY, WEEK, MONTH, YEAR 중 하나여야 합니다."
            );
        }
        if (from == null || to == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "from, to 날짜는 필수입니다.");
        }
        if (from.isAfter(to)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "from 날짜는 to 날짜보다 이후일 수 없습니다.");
        }

        LocalDate today = LocalDate.now(KST);
        if (to.isAfter(today)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "미래 날짜는 조회할 수 없습니다.");
        }
    }

    private record DateBucket(
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    private record DailyCount(
            long sessionCount,
            long drowsyCount,
            long sleepCount,
            long totalRiskCount
    ) {
        private static DailyCount from(OrganizationDailyRiskStat stat) {
            return new DailyCount(
                    stat.getSessionCount(),
                    stat.getDrowsyCount(),
                    stat.getSleepCount(),
                    stat.getTotalRiskCount()
            );
        }
    }

    private static class TopMemberAccumulator {
        private String name;
        private long totalRiskCount;

        private TopMemberAccumulator(String name, long totalRiskCount) {
            this.name = name;
            this.totalRiskCount = totalRiskCount;
        }
    }
}
