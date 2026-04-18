package ac.jwooo.eye_on.domain.monitoring.domain.service;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;

public interface MonitoringService {

    MonitoringSessionStartResponse startSession(Long userId, StartMonitoringSessionRequest request);

    MonitoringSessionEndResponse endSession(Long userId, Long sessionId, EndMonitoringSessionRequest request);

    MonitoringEventResponse createEvent(Long userId, Long sessionId, CreateMonitoringEventRequest request);

    MonitoringRealtimeSummaryResponse getRealtimeSummary(Long userId);
}
