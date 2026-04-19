package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MonitoringHourlyRisk24hResponse(
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        List<MonitoringHourlyRiskBucket> buckets
) {
    public record MonitoringHourlyRiskBucket(
            LocalDateTime bucketStart,
            LocalDateTime bucketEnd,
            long totalRiskCount
    ) {
    }
}
