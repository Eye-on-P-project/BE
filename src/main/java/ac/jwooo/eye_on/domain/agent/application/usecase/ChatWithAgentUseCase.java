package ac.jwooo.eye_on.domain.agent.application.usecase;

import ac.jwooo.eye_on.domain.agent.application.dto.request.AgentChatRequest;
import ac.jwooo.eye_on.domain.agent.application.dto.response.AgentChatResponse;
import ac.jwooo.eye_on.domain.agent.domain.service.AgentSubscriptionService;
import ac.jwooo.eye_on.domain.agent.domain.service.GeminiAgentClient;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatWithAgentUseCase {

    private final AgentSubscriptionService agentSubscriptionService;
    private final GeminiAgentClient geminiAgentClient;

    @Transactional(readOnly = true)
    public AgentChatResponse execute(Long userId, AgentChatRequest request) {
        if (!agentSubscriptionService.canUseAgent(userId)) {
            throw new CustomException(ErrorCode.AGENT_SUBSCRIPTION_REQUIRED);
        }

        String reply = geminiAgentClient.generateReply(request.drivingState(), request.message());
        return new AgentChatResponse(reply);
    }
}
