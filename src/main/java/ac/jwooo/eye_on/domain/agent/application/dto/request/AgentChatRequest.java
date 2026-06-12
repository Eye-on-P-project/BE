package ac.jwooo.eye_on.domain.agent.application.dto.request;

import ac.jwooo.eye_on.domain.agent.domain.entity.AgentDrivingState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        @NotBlank(message = "메시지는 필수입니다.")
        @Size(max = 1200, message = "메시지는 1200자 이하여야 합니다.")
        String message,

        @NotNull(message = "운전자 상태는 필수입니다.")
        AgentDrivingState drivingState
) {
}
