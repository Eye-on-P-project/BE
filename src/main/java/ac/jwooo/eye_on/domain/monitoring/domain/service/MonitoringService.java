package ac.jwooo.eye_on.domain.monitoring.domain.service;

import java.util.List;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringHourlyRisk24hResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringNotificationPageResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRecentEndedSessionResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MonitoringService {

    MonitoringSessionStartResponse startSession(Long userId, StartMonitoringSessionRequest request);

    MonitoringSessionEndResponse endSession(Long userId, Long sessionId, EndMonitoringSessionRequest request);

    MonitoringEventResponse createEvent(Long userId, Long sessionId, CreateMonitoringEventRequest request);

    MonitoringRealtimeSummaryResponse getRealtimeSummary(Long userId);

    SseEmitter subscribeRealtimeSummary(Long userId);

    MonitoringHourlyRisk24hResponse getHourlyRisk24h(Long userId);

    List<MonitoringRecentEndedSessionResponse> getRecentEndedSessions(Long userId, int limit);

    MonitoringNotificationPageResponse getRecentNotifications(Long userId, Long cursor, int limit);
}
