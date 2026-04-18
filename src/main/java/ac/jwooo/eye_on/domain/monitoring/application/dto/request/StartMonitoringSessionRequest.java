package ac.jwooo.eye_on.domain.monitoring.application.dto.request;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringMode;
import jakarta.validation.constraints.NotNull;

public record StartMonitoringSessionRequest(
        @NotNull(message = "모니터링 모드는 필수입니다.")
        MonitoringMode mode,

        @NotNull(message = "앱 시작 시각은 필수입니다.")
        LocalDateTime startedAtApp
) {
}

