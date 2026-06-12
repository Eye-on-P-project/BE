package ac.jwooo.eye_on.domain.agent.application.usecase;

import ac.jwooo.eye_on.domain.agent.application.dto.response.AgentConfigResponse;
import ac.jwooo.eye_on.domain.agent.domain.entity.AgentMode;
import ac.jwooo.eye_on.domain.agent.domain.service.AgentSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetAgentConfigUseCase {

    private static final int DEFAULT_COOLDOWN_SECONDS = 30;

    private final AgentSubscriptionService agentSubscriptionService;

    @Transactional(readOnly = true)
    public AgentConfigResponse execute(Long userId) {
        boolean enabled = agentSubscriptionService.canUseAgent(userId);
        if (!enabled) {
            return new AgentConfigResponse(false, AgentMode.PASSIVE, 0);
        }
        return new AgentConfigResponse(true, AgentMode.PROACTIVE, DEFAULT_COOLDOWN_SECONDS);
    }
}
