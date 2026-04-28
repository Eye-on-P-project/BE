package ac.jwooo.eye_on.domain.organization.application.dto.response;

import ac.jwooo.eye_on.domain.monitoring.domain.repository.OrganizationRiskUserProjection;
import com.fasterxml.jackson.annotation.JsonFormat;

public record OrganizationRiskUserResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,
        String email,
        String name,
        String nickname,
        long totalSessionCount,
        long drowsyCount,
        long sleepCount,
        long totalRiskCount,
        boolean isMonitoringActive
) {
    public static OrganizationRiskUserResponse from(OrganizationRiskUserProjection projection) {
        return new OrganizationRiskUserResponse(
                projection.getUserId(),
                projection.getEmail(),
                projection.getName(),
                projection.getNickname(),
                nullSafe(projection.getTotalSessionCount()),
                nullSafe(projection.getDrowsyCount()),
                nullSafe(projection.getSleepCount()),
                nullSafe(projection.getTotalRiskCount()),
                nullSafeBoolean(projection.getIsMonitoringActive())
        );
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private static boolean nullSafeBoolean(Integer value) {
        return value != null && value != 0;
    }
}
