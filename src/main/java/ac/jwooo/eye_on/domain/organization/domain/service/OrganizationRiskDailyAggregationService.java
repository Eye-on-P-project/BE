package ac.jwooo.eye_on.domain.organization.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringEventLogRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.OrganizationRiskCountProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.OrganizationSessionCountProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.OrganizationUserRiskCountProjection;
import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationDailyRiskStat;
import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationUserDailyRiskStat;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationDailyRiskStatRepository;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationUserDailyRiskStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationRiskDailyAggregationService {

    private final MonitoringSessionRepository monitoringSessionRepository;
    private final MonitoringEventLogRepository monitoringEventLogRepository;
    private final OrganizationDailyRiskStatRepository organizationDailyRiskStatRepository;
    private final OrganizationUserDailyRiskStatRepository organizationUserDailyRiskStatRepository;

    @Transactional
    public void aggregateForDate(LocalDate statDate) {
        LocalDateTime dayStart = statDate.atStartOfDay();
        LocalDateTime dayEndExclusive = dayStart.plusDays(1);

        Map<Long, DailyAccumulator> organizationDailyAccumulator = new HashMap<>();

        for (OrganizationSessionCountProjection projection : monitoringSessionRepository
                .findOrganizationSessionCountsByRange(dayStart, dayEndExclusive)) {
            DailyAccumulator accumulator = organizationDailyAccumulator
                    .computeIfAbsent(projection.getOrganizationId(), ignored -> new DailyAccumulator());
            accumulator.sessionCount = nullSafe(projection.getSessionCount());
        }

        for (OrganizationRiskCountProjection projection : monitoringEventLogRepository
                .findOrganizationRiskCountsByRange(dayStart, dayEndExclusive)) {
            DailyAccumulator accumulator = organizationDailyAccumulator
                    .computeIfAbsent(projection.getOrganizationId(), ignored -> new DailyAccumulator());
            accumulator.drowsyCount = nullSafe(projection.getDrowsyCount());
            accumulator.sleepCount = nullSafe(projection.getSleepCount());
            accumulator.totalRiskCount = nullSafe(projection.getTotalRiskCount());
        }

        List<OrganizationDailyRiskStat> orgDailyStats = new ArrayList<>();
        for (Map.Entry<Long, DailyAccumulator> entry : organizationDailyAccumulator.entrySet()) {
            DailyAccumulator value = entry.getValue();
            orgDailyStats.add(OrganizationDailyRiskStat.create(
                    entry.getKey(),
                    statDate,
                    value.sessionCount,
                    value.drowsyCount,
                    value.sleepCount,
                    value.totalRiskCount
            ));
        }

        List<OrganizationUserDailyRiskStat> orgUserDailyStats = monitoringEventLogRepository
                .findOrganizationUserRiskCountsByRange(dayStart, dayEndExclusive)
                .stream()
                .map(projection -> OrganizationUserDailyRiskStat.create(
                        projection.getOrganizationId(),
                        projection.getUserId(),
                        statDate,
                        nullSafe(projection.getDrowsyCount()),
                        nullSafe(projection.getSleepCount()),
                        nullSafe(projection.getTotalRiskCount())
                ))
                .toList();

        organizationDailyRiskStatRepository.deleteAllByStatDate(statDate);
        organizationUserDailyRiskStatRepository.deleteAllByStatDate(statDate);

        organizationDailyRiskStatRepository.saveAll(orgDailyStats);
        organizationUserDailyRiskStatRepository.saveAll(orgUserDailyStats);
    }

    @Transactional
    public void aggregateRecentDays(int days) {
        LocalDate today = LocalDate.now();
        for (int offset = 1; offset <= days; offset++) {
            aggregateForDate(today.minusDays(offset));
        }
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private static class DailyAccumulator {
        private long sessionCount;
        private long drowsyCount;
        private long sleepCount;
        private long totalRiskCount;
    }
}
