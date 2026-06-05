package ac.jwooo.eye_on.domain.monitoring.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
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
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringNotificationResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringNotificationPageResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRecentEndedSessionResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventLog;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventType;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringMode;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.Notification;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.NotificationType;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringEventLogRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRealtimeSummaryProjection;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.NotificationRepository;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.TimeBucketRiskCountProjection;
import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationMemberRepository;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringServiceImpl implements MonitoringService {

    private static final int MAX_RECENT_NOTIFICATIONS = 200;

    private final MonitoringSessionRepository monitoringSessionRepository;
    private final MonitoringEventLogRepository monitoringEventLogRepository;
    private final NotificationRepository notificationRepository;
    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final MonitoringRealtimeSseBroker monitoringRealtimeSseBroker;

    @Override
    @Transactional
    public MonitoringSessionStartResponse startSession(Long userId, StartMonitoringSessionRequest request) {
        LocalDateTime now = nowWithoutNanos();
        List<MonitoringSession> activeSessions = monitoringSessionRepository
                .findByUserIdAndEndedAtServerIsNullAndDeletedAtIsNull(userId);

        // 모바일 네트워크/토큰 이슈로 종료 요청이 누락된 경우를 복구하기 위해
        // 기존 active 세션들을 현재 시각으로 종료하고 새 세션을 시작한다.
        for (MonitoringSession activeSession : activeSessions) {
            LocalDateTime endedAtApp = now.isBefore(activeSession.getStartedAtApp())
                    ? activeSession.getStartedAtApp()
                    : now;
            long durationMinutesLong = Duration.between(activeSession.getStartedAtApp(), endedAtApp).toMinutes();
            int durationMinutes = (int) Math.min(Math.max(durationMinutesLong, 0L), Integer.MAX_VALUE);
            activeSession.end(endedAtApp, now, durationMinutes);
            publishRealtimeSummaryUpdate(activeSession, null, null);
        }

        LocalDateTime startedAtApp = truncateToSeconds(request.startedAtApp());
        Long organizationId = resolveOrganizationIdForSessionStart(userId, request.mode());

        MonitoringSession monitoringSession = MonitoringSession.create(
                userId,
                organizationId,
                request.mode(),
                startedAtApp,
                now
        );

        MonitoringSession savedMonitoringSession = monitoringSessionRepository.save(monitoringSession);
        publishRealtimeSummaryUpdate(savedMonitoringSession, null, null);
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
        publishRealtimeSummaryUpdate(monitoringSession, null, null);
        return MonitoringSessionEndResponse.from(monitoringSession);
    }

    @Override
    @Transactional
    public MonitoringEventResponse createEvent(Long userId, Long sessionId, CreateMonitoringEventRequest request) {
        MonitoringSession monitoringSession = getOwnedSession(userId, sessionId);
        LocalDateTime now = nowWithoutNanos();
        LocalDateTime occurredAtApp = now;
        LocalDateTime occurredAtServer = now;
        MonitoringEventType eventType = request.eventType();

        if (occurredAtServer.isBefore(monitoringSession.getStartedAtServer())) {
            throw new CustomException(ErrorCode.INVALID_MONITORING_TIME_RANGE);
        }
        if (monitoringSession.getEndedAtServer() != null
                && occurredAtServer.isAfter(monitoringSession.getEndedAtServer())) {
            throw new CustomException(ErrorCode.INVALID_MONITORING_TIME_RANGE);
        }

        monitoringEventLogRepository.findTopBySessionIdAndDeletedAtIsNullOrderByOccurredAtAppDescIdDesc(sessionId)
                .ifPresent(lastEvent -> {
                    if (occurredAtServer.isBefore(lastEvent.getOccurredAtServer())) {
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
                occurredAtServer
        );

        MonitoringEventLog savedMonitoringEventLog = monitoringEventLogRepository.save(monitoringEventLog);
        MonitoringEventResponse eventResponse = MonitoringEventResponse.from(savedMonitoringEventLog, monitoringSession);
        MonitoringNotificationResponse notificationResponse = createAlertResponse(monitoringSession, eventResponse);
        publishRealtimeSummaryUpdate(monitoringSession, eventResponse, notificationResponse);
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

        LocalDateTime now = nowWithoutNanos();
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

    @Override
    public List<MonitoringRecentEndedSessionResponse> getRecentEndedSessions(Long userId, int limit) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(userId).getId();
        int normalizedLimit = Math.max(1, Math.min(limit, 100));

        return monitoringSessionRepository.findRecentEndedSessionsByOrganizationId(organizationId, normalizedLimit).stream()
                .map(MonitoringRecentEndedSessionResponse::from)
                .toList();
    }

    @Override
    public MonitoringNotificationPageResponse getRecentNotifications(Long userId, Long cursor, int limit) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(userId).getId();
        int normalizedLimit = Math.max(1, Math.min(limit, MAX_RECENT_NOTIFICATIONS));
        int fetchLimit = normalizedLimit + 1;

        List<MonitoringNotificationResponse> notifications = notificationRepository
                .findRecentByOrganizationIdWithCursor(organizationId, cursor, fetchLimit).stream()
                .map(MonitoringNotificationResponse::fromProjection)
                .toList();

        boolean hasNext = notifications.size() > normalizedLimit;
        List<MonitoringNotificationResponse> items = hasNext
                ? notifications.subList(0, normalizedLimit)
                : notifications;
        Long nextCursor = hasNext && !items.isEmpty()
                ? items.get(items.size() - 1).notificationId()
                : null;

        return new MonitoringNotificationPageResponse(items, nextCursor, hasNext);
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

    private void publishRealtimeSummaryUpdate(
            MonitoringSession monitoringSession,
            MonitoringEventResponse eventResponse,
            MonitoringNotificationResponse notificationResponse
    ) {
        if (monitoringSession.getMode() != MonitoringMode.ORGANIZATION) {
            return;
        }

        Long organizationId = monitoringSession.getOrganizationId();
        if (organizationId == null) {
            return;
        }

        runAfterCommit(() -> {
            MonitoringRealtimeSummaryResponse summary = getRealtimeSummaryByOrganizationId(organizationId);
            monitoringRealtimeSseBroker.sendSummary(organizationId, summary);

            if (eventResponse == null) {
                return;
            }
            if (notificationResponse != null) {
                monitoringRealtimeSseBroker.sendAlert(organizationId, notificationResponse);
            }
        });
    }

    private MonitoringNotificationResponse createAlertResponse(
            MonitoringSession monitoringSession,
            MonitoringEventResponse eventResponse
    ) {
        if (monitoringSession.getMode() != MonitoringMode.ORGANIZATION) {
            return null;
        }

        User sourceUser = userRepository.findByIdAndDeletedAtIsNull(monitoringSession.getUserId()).orElse(null);
        String sourceUserName = resolveDisplayName(sourceUser, monitoringSession.getUserId());
        NotificationType notificationType = NotificationType.fromMonitoringEventType(eventResponse.eventType());

        if (notificationType == NotificationType.NORMAL) {
            return MonitoringNotificationResponse.ofStream(
                    monitoringSession.getUserId(),
                    monitoringSession.getUserId(),
                    sourceUserName,
                    notificationType,
                    buildNotificationContent(sourceUserName, notificationType),
                    eventResponse.occurredAtServer()
            );
        }
        if (!isRiskEvent(eventResponse.eventType())) {
            return null;
        }

        String content = buildNotificationContent(sourceUserName, notificationType);
        Notification savedNotification = notificationRepository.save(Notification.create(
                monitoringSession.getUserId(),
                monitoringSession.getUserId(),
                content,
                notificationType
        ));

        return MonitoringNotificationResponse.fromEntity(
                savedNotification,
                sourceUserName,
                eventResponse.occurredAtServer()
        );
    }

    private String resolveDisplayName(User user, Long userId) {
        if (user == null) {
            return "사용자 " + userId;
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName().trim();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail().trim();
        }
        return "사용자 " + userId;
    }

    private boolean isRiskEvent(MonitoringEventType eventType) {
        return eventType == MonitoringEventType.DROWSY || eventType == MonitoringEventType.SLEEP;
    }

    private String buildNotificationContent(String sourceUserName, NotificationType notificationType) {
        if (notificationType == NotificationType.NORMAL) {
            return sourceUserName + " 사용자가 정상 상태로 복귀했습니다.";
        }
        if (notificationType == NotificationType.SLEEP) {
            return sourceUserName + " 사용자에게 수면 상태 경고가 감지되었습니다.";
        }
        return sourceUserName + " 사용자에게 졸음 의심 경고가 감지되었습니다.";
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

    private Long resolveOrganizationIdForSessionStart(Long userId, MonitoringMode mode) {
        if (mode != MonitoringMode.ORGANIZATION) {
            return null;
        }

        return organizationMemberRepository.findFirstByUserIdAndDeletedAtIsNull(userId)
                .map(OrganizationMember::getOrganizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_MEMBER_NOT_FOUND));
    }
}
