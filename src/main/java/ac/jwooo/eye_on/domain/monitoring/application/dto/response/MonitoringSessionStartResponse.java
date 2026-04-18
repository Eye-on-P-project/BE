package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringMode;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MonitoringSessionStartResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,
        MonitoringMode mode,
        LocalDateTime startedAtApp,
        LocalDateTime startedAtServer,
        Integer drowsyCount,
        Integer sleepCount
) {
    public static MonitoringSessionStartResponse from(MonitoringSession monitoringSession) {
        return new MonitoringSessionStartResponse(
                monitoringSession.getId(),
                monitoringSession.getUserId(),
                monitoringSession.getMode(),
                monitoringSession.getStartedAtApp(),
                monitoringSession.getStartedAtServer(),
                monitoringSession.getDrowsyCount(),
                monitoringSession.getSleepCount()
        );
    }
}

