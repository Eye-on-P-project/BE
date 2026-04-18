package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventLog;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventType;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MonitoringEventResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long eventId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long sessionId,
        MonitoringEventType eventType,
        LocalDateTime occurredAtApp,
        LocalDateTime occurredAtServer,
        LocalDateTime resolvedAtApp,
        LocalDateTime resolvedAtServer,
        BigDecimal durationSeconds,
        Integer drowsyCount,
        Integer sleepCount
) {
    public static MonitoringEventResponse from(
            MonitoringEventLog monitoringEventLog,
            MonitoringSession monitoringSession
    ) {
        return new MonitoringEventResponse(
                monitoringEventLog.getId(),
                monitoringEventLog.getSessionId(),
                monitoringEventLog.getEventType(),
                monitoringEventLog.getOccurredAtApp(),
                monitoringEventLog.getOccurredAtServer(),
                monitoringEventLog.getResolvedAtApp(),
                monitoringEventLog.getResolvedAtServer(),
                monitoringEventLog.getDurationSeconds(),
                monitoringSession.getDrowsyCount(),
                monitoringSession.getSleepCount()
        );
    }
}
