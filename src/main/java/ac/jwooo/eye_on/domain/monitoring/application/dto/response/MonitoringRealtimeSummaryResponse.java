package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRealtimeSummaryProjection;

public record MonitoringRealtimeSummaryResponse(
        long totalMemberCount,
        long activeSessionCount,
        long warningSessionCount,
        long drowsyWarningSessionCount,
        long sleepWarningSessionCount
) {
    public static MonitoringRealtimeSummaryResponse from(MonitoringSessionRealtimeSummaryProjection projection) {
        if (projection == null) {
            return new MonitoringRealtimeSummaryResponse(0L, 0L, 0L, 0L, 0L);
        }

        return new MonitoringRealtimeSummaryResponse(
                nullSafe(projection.getTotalMemberCount()),
                nullSafe(projection.getActiveSessionCount()),
                nullSafe(projection.getWarningSessionCount()),
                nullSafe(projection.getDrowsyWarningSessionCount()),
                nullSafe(projection.getSleepWarningSessionCount())
        );
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
