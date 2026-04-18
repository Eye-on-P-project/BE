package ac.jwooo.eye_on.domain.monitoring.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventLog;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventType;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringEventLogRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringServiceImpl implements MonitoringService {

    private final MonitoringSessionRepository monitoringSessionRepository;
    private final MonitoringEventLogRepository monitoringEventLogRepository;

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

        if (eventType == MonitoringEventType.NORMAL) {
            return resolveMonitoringEvent(monitoringSession, occurredAtApp, request.eventId());
        }

        if (request.eventId() != null) {
            throw new CustomException(
                    ErrorCode.INVALID_MONITORING_EVENT_REQUEST,
                    "DROWSY/SLEEP 이벤트에는 eventId를 보내지 않아야 합니다."
            );
        }

        monitoringSession.increaseEventCount(eventType);

        MonitoringEventLog monitoringEventLog = MonitoringEventLog.create(
                monitoringSession.getId(),
                eventType,
                occurredAtApp,
                nowWithoutNanos()
        );

        MonitoringEventLog savedMonitoringEventLog = monitoringEventLogRepository.save(monitoringEventLog);
        return MonitoringEventResponse.from(savedMonitoringEventLog, monitoringSession);
    }

    private MonitoringSession getOwnedSession(Long userId, Long sessionId) {
        MonitoringSession monitoringSession = monitoringSessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MONITORING_SESSION_NOT_FOUND));

        if (!Objects.equals(monitoringSession.getUserId(), userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 모니터링 세션만 접근할 수 있습니다.");
        }

        return monitoringSession;
    }

    private LocalDateTime nowWithoutNanos() {
        return LocalDateTime.now().withNano(0);
    }

    private LocalDateTime truncateToSeconds(LocalDateTime value) {
        return value.withNano(0);
    }

    private MonitoringEventResponse resolveMonitoringEvent(
            MonitoringSession monitoringSession,
            LocalDateTime normalOccurredAtApp,
            Long eventId
    ) {
        if (eventId == null) {
            throw new CustomException(
                    ErrorCode.INVALID_MONITORING_EVENT_REQUEST,
                    "NORMAL 이벤트에는 종료할 eventId가 필요합니다."
            );
        }

        MonitoringEventLog monitoringEventLog = monitoringEventLogRepository
                .findByIdAndSessionIdAndDeletedAtIsNull(eventId, monitoringSession.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MONITORING_EVENT_NOT_FOUND));

        if (monitoringEventLog.getEventType() == MonitoringEventType.NORMAL) {
            throw new CustomException(
                    ErrorCode.INVALID_MONITORING_EVENT_REQUEST,
                    "NORMAL 타입 이벤트 로그는 종료 대상으로 사용할 수 없습니다."
            );
        }

        if (monitoringEventLog.isResolved()) {
            throw new CustomException(ErrorCode.MONITORING_EVENT_ALREADY_RESOLVED);
        }

        if (normalOccurredAtApp.isBefore(monitoringEventLog.getOccurredAtApp())) {
            throw new CustomException(ErrorCode.INVALID_MONITORING_TIME_RANGE);
        }

        BigDecimal durationSeconds = calculateDurationSeconds(
                monitoringEventLog.getOccurredAtApp(),
                normalOccurredAtApp
        );

        monitoringEventLog.resolve(normalOccurredAtApp, nowWithoutNanos(), durationSeconds);
        return MonitoringEventResponse.from(monitoringEventLog, monitoringSession);
    }

    private BigDecimal calculateDurationSeconds(LocalDateTime start, LocalDateTime end) {
        long durationMillis = Duration.between(start, end).toMillis();
        return BigDecimal.valueOf(durationMillis)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }
}
