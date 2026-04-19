package ac.jwooo.eye_on.domain.monitoring.application.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record EndMonitoringSessionRequest(
        @NotNull(message = "앱 종료 시각은 필수입니다.")
        LocalDateTime endedAtApp
) {
}

