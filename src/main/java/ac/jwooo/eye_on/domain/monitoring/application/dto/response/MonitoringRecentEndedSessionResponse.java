package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringRecentEndedSessionProjection;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MonitoringRecentEndedSessionResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,
        String userName,
        LocalDateTime startedAtApp,
        LocalDateTime endedAtApp,
        int durationMinutes,
        long drowsyCount,
        long sleepCount,
        long totalRiskCount
) {
    public static MonitoringRecentEndedSessionResponse from(MonitoringRecentEndedSessionProjection projection) {
        return new MonitoringRecentEndedSessionResponse(
                projection.getSessionId(),
                projection.getUserId(),
                projection.getUserName(),
                projection.getStartedAtApp(),
                projection.getEndedAtApp(),
                nullSafe(projection.getDurationMinutes()),
                nullSafe(projection.getDrowsyCount()),
                nullSafe(projection.getSleepCount()),
                nullSafe(projection.getTotalRiskCount())
        );
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
