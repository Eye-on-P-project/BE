package ac.jwooo.eye_on.domain.monitoring.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringHourlyRisk24hResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventLog;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventType;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringMode;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringEventLogRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRealtimeSummaryProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.TimeBucketRiskCountProjection;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationMemberRepository;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringServiceImpl implements MonitoringService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MonitoringSessionRepository monitoringSessionRepository;
    private final MonitoringEventLogRepository monitoringEventLogRepository;
    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final MonitoringRealtimeSseBroker monitoringRealtimeSseBroker;

    @Override
    @Transactional
    public MonitoringSessionStartResponse startSession(Long userId, StartMonitoringSessionRequest request) {
        if (monitoringSessionRepository.existsByUserIdAndEndedAtServerIsNullAndDeletedAtIsNull(userId)) {
            throw new CustomException(ErrorCode.MONITORING_SESSION_ALREADY_ACTIVE);
        }

        LocalDateTime startedAtApp = truncateToSeconds(request.startedAtApp());

        MonitoringSession monitoringSession = MonitoringSession.create(
                userId,
                request.mode(),
                startedAtApp,
                nowWithoutNanos()
        );

        MonitoringSession savedMonitoringSession = monitoringSessionRepository.save(monitoringSession);
        publishRealtimeSummaryUpdate(savedMonitoringSession, null);
        return MonitoringSessionStartResponse.from(savedMonitoringSession);
    }

    @Override
    @Transactional
    public MonitoringSessionEndResponse endSession(Long userId, Long sessionId, EndMonitoringSessionRequest request) {
        MonitoringSession monitoringSession = getOwnedSession(userId, sessionId);
        LocalDateTime endedAtApp = truncateToSeconds(request.endedAtApp());

        if (monitoringSession.isEnded()) {
            throw new CustomException(ErrorCode.MONITORING_SESSION_ALREADY_ENDED);
        }
        if (endedAtApp.isBefore(monitoringSession.getStartedAtApp())) {
            throw new CustomException(ErrorCode.INVALID_MONITORING_TIME_RANGE);
        }

        long durationMinutesLong = Duration.between(monitoringSession.getStartedAtApp(), endedAtApp).toMinutes();
        int durationMinutes = (int) Math.min(durationMinutesLong, Integer.MAX_VALUE);

        monitoringSession.end(endedAtApp, nowWithoutNanos(), durationMinutes);
        publishRealtimeSummaryUpdate(monitoringSession, null);
        return MonitoringSessionEndResponse.from(monitoringSession);
    }

    @Override
    @Transactional
    public MonitoringEventResponse createEvent(Long userId, Long sessionId, CreateMonitoringEventRequest request) {
        MonitoringSession monitoringSession = getOwnedSession(userId, sessionId);
        LocalDateTime occurredAtApp = truncateToSeconds(request.occurredAtApp());
        MonitoringEventType eventType = request.eventType();

        if (occurredAtApp.isBefore(monitoringSession.getStartedAtApp())) {
            throw new CustomException(ErrorCode.INVALID_MONITORING_TIME_RANGE);
        }
        if (monitoringSession.getEndedAtApp() != null
                && occurredAtApp.isAfter(monitoringSession.getEndedAtApp())) {
            throw new CustomException(ErrorCode.INVALID_MONITORING_TIME_RANGE);
        }

        monitoringEventLogRepository.findTopBySessionIdAndDeletedAtIsNullOrderByOccurredAtAppDescIdDesc(sessionId)
                .ifPresent(lastEvent -> {
                    if (occurredAtApp.isBefore(lastEvent.getOccurredAtApp())) {
                        throw new CustomException(
                                ErrorCode.INVALID_MONITORING_TIME_RANGE,
                                "이벤트 시각은 같은 세션의 이전 이벤트보다 빠를 수 없습니다."
                        );
                    }
                });

        if (eventType != MonitoringEventType.NORMAL) {
            monitoringSession.increaseEventCount(eventType);
        }

        MonitoringEventLog monitoringEventLog = MonitoringEventLog.create(
                monitoringSession.getId(),
                eventType,
                occurredAtApp,
                nowWithoutNanos()
        );

        MonitoringEventLog savedMonitoringEventLog = monitoringEventLogRepository.save(monitoringEventLog);
        MonitoringEventResponse eventResponse = MonitoringEventResponse.from(savedMonitoringEventLog, monitoringSession);
        publishRealtimeSummaryUpdate(monitoringSession, eventResponse);
        return eventResponse;
    }

    @Override
    public MonitoringRealtimeSummaryResponse getRealtimeSummary(Long userId) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(userId).getId();
        return getRealtimeSummaryByOrganizationId(organizationId);
    }

    @Override
    public SseEmitter subscribeRealtimeSummary(Long userId) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(userId).getId();
        SseEmitter emitter = monitoringRealtimeSseBroker.connect(organizationId);
        monitoringRealtimeSseBroker.sendSummary(organizationId, getRealtimeSummaryByOrganizationId(organizationId));
        return emitter;
    }

    @Override
    public MonitoringHourlyRisk24hResponse getHourlyRisk24h(Long userId) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(userId).getId();

        LocalDateTime now = LocalDateTime.now(KST).withNano(0);
        LocalDateTime endExclusive = now.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
        LocalDateTime rangeStart = endExclusive.minusHours(24);

        Map<LocalDateTime, Long> countsByHour = new HashMap<>();
        for (TimeBucketRiskCountProjection projection : monitoringEventLogRepository
                .findHourlyRiskCountsByOrganizationAndRange(organizationId, rangeStart, endExclusive)) {
            LocalDateTime bucketStart = LocalDateTime.of(
                    projection.getYear(),
                    projection.getMonth(),
                    projection.getDay(),
                    projection.getHour(),
                    0,
                    0
            );
            countsByHour.put(bucketStart, projection.getTotalRiskCount() == null ? 0L : projection.getTotalRiskCount());
        }

        List<MonitoringHourlyRisk24hResponse.MonitoringHourlyRiskBucket> buckets = new ArrayList<>();
        LocalDateTime cursor = rangeStart;
        while (cursor.isBefore(endExclusive)) {
            LocalDateTime bucketEndExclusive = cursor.plusHours(1);
            buckets.add(new MonitoringHourlyRisk24hResponse.MonitoringHourlyRiskBucket(
                    cursor,
                    bucketEndExclusive.minusSeconds(1),
                    countsByHour.getOrDefault(cursor, 0L)
            ));
            cursor = bucketEndExclusive;
        }

        return new MonitoringHourlyRisk24hResponse(
                rangeStart,
                endExclusive.minusSeconds(1),
                buckets
        );
    }

    private MonitoringSession getOwnedSession(Long userId, Long sessionId) {
        MonitoringSession monitoringSession = monitoringSessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MONITORING_SESSION_NOT_FOUND));

        if (!Objects.equals(monitoringSession.getUserId(), userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 모니터링 세션만 접근할 수 있습니다.");
        }

        return monitoringSession;
    }

    private MonitoringRealtimeSummaryResponse getRealtimeSummaryByOrganizationId(Long organizationId) {
        MonitoringSessionRealtimeSummaryProjection projection = monitoringSessionRepository
                .findRealtimeSummaryByOrganizationId(organizationId);
        return MonitoringRealtimeSummaryResponse.from(projection);
    }

    private void publishRealtimeSummaryUpdate(MonitoringSession monitoringSession, MonitoringEventResponse eventResponse) {
        if (monitoringSession.getMode() != MonitoringMode.ORGANIZATION) {
            return;
        }

        Long organizationId = organizationMemberRepository.findFirstByUserIdAndDeletedAtIsNull(monitoringSession.getUserId())
                .map(member -> member.getOrganizationId())
                .orElse(null);
        if (organizationId == null) {
            return;
        }

        runAfterCommit(() -> {
            MonitoringRealtimeSummaryResponse summary = getRealtimeSummaryByOrganizationId(organizationId);
            monitoringRealtimeSseBroker.sendSummary(organizationId, summary);

            if (eventResponse == null) {
                return;
            }
            if (eventResponse.eventType() == MonitoringEventType.DROWSY
                    || eventResponse.eventType() == MonitoringEventType.SLEEP) {
                monitoringRealtimeSseBroker.sendAlert(organizationId, eventResponse);
            }
        });
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private LocalDateTime nowWithoutNanos() {
        return LocalDateTime.now().withNano(0);
    }

    private LocalDateTime truncateToSeconds(LocalDateTime value) {
        return value.withNano(0);
    }
}
