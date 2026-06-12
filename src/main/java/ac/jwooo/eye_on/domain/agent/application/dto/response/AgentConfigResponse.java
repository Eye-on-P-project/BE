package ac.jwooo.eye_on.domain.agent.application.dto.response;

import ac.jwooo.eye_on.domain.agent.domain.entity.AgentMode;

public record AgentConfigResponse(
        boolean enabled,
        AgentMode mode,
        int cooldownSeconds
) {
}
