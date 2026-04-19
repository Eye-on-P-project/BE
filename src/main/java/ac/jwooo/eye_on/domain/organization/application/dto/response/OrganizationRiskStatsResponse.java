package ac.jwooo.eye_on.domain.organization.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ac.jwooo.eye_on.domain.organization.application.dto.request.RiskStatsGranularity;
import com.fasterxml.jackson.annotation.JsonFormat;

public record OrganizationRiskStatsResponse(
        RiskStatsGranularity granularity,
        LocalDate from,
        LocalDate to,
        List<RiskStatsBucket> series,
        List<RiskTopMember> top5Members
) {
    public record RiskStatsBucket(
            LocalDateTime bucketStart,
            LocalDateTime bucketEnd,
            long sessionCount,
            long drowsyCount,
            long sleepCount,
            long totalRiskCount
    ) {
    }

    public record RiskTopMember(
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Long userId,
            String name,
            long totalRiskCount
    ) {
    }
}
