package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringMode;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MonitoringSessionEndResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,
        MonitoringMode mode,
        LocalDateTime startedAtApp,
        LocalDateTime startedAtServer,
        LocalDateTime endedAtApp,
        LocalDateTime endedAtServer,
        Integer durationMinutes,
        Integer drowsyCount,
        Integer sleepCount
) {
    public static MonitoringSessionEndResponse from(MonitoringSession monitoringSession) {
        return new MonitoringSessionEndResponse(
                monitoringSession.getId(),
                monitoringSession.getUserId(),
                monitoringSession.getMode(),
                monitoringSession.getStartedAtApp(),
                monitoringSession.getStartedAtServer(),
                monitoringSession.getEndedAtApp(),
                monitoringSession.getEndedAtServer(),
                monitoringSession.getDurationMinutes(),
                monitoringSession.getDrowsyCount(),
                monitoringSession.getSleepCount()
        );
    }
}

