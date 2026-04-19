package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record MonitoringNotificationPageResponse(
        List<MonitoringNotificationResponse> items,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long nextCursor,
        boolean hasNext
) {
}
