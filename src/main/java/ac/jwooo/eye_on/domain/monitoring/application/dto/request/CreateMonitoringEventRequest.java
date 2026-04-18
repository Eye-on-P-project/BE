package ac.jwooo.eye_on.domain.monitoring.application.dto.request;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMonitoringEventRequest(
        @NotNull(message = "이벤트 타입은 필수입니다.")
        MonitoringEventType eventType,

        @NotNull(message = "앱 이벤트 발생 시각은 필수입니다.")
        LocalDateTime occurredAtApp,

        @Positive(message = "eventId는 1 이상이어야 합니다.")
        Long eventId
) {
}
