package ac.jwooo.eye_on.domain.monitoring.ui;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringHourlyRisk24hResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;
import ac.jwooo.eye_on.domain.monitoring.domain.service.MonitoringService;
import ac.jwooo.eye_on.domain.monitoring.ui.spec.MonitoringControllerSpec;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController implements MonitoringControllerSpec {

    private final MonitoringService monitoringService;

    @GetMapping("/dashboard/realtime-summary")
    public MonitoringRealtimeSummaryResponse getRealtimeSummary(Authentication authentication) {
        return monitoringService.getRealtimeSummary(extractUserId(authentication));
    }

    @GetMapping(value = "/dashboard/realtime-summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeRealtimeSummary(Authentication authentication) {
        return monitoringService.subscribeRealtimeSummary(extractUserId(authentication));
    }

    @GetMapping("/dashboard/hourly-risk-24h")
    public MonitoringHourlyRisk24hResponse getHourlyRisk24h(Authentication authentication) {
        return monitoringService.getHourlyRisk24h(extractUserId(authentication));
    }

    @PostMapping("/sessions/start")
    public MonitoringSessionStartResponse startMonitoring(
            Authentication authentication,
            @Valid @RequestBody StartMonitoringSessionRequest request
    ) {
        return monitoringService.startSession(extractUserId(authentication), request);
    }

    @PostMapping("/sessions/{sessionId}/end")
    public MonitoringSessionEndResponse endMonitoring(
            Authentication authentication,
            @PathVariable Long sessionId,
            @Valid @RequestBody EndMonitoringSessionRequest request
    ) {
        return monitoringService.endSession(extractUserId(authentication), sessionId, request);
    }

    @PostMapping("/sessions/{sessionId}/events")
    public MonitoringEventResponse createMonitoringEvent(
            Authentication authentication,
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateMonitoringEventRequest request
    ) {
        return monitoringService.createEvent(extractUserId(authentication), sessionId, request);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
