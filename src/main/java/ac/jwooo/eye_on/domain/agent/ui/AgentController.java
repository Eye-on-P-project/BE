package ac.jwooo.eye_on.domain.agent.ui;

import ac.jwooo.eye_on.domain.agent.application.dto.request.AgentChatRequest;
import ac.jwooo.eye_on.domain.agent.application.dto.response.AgentChatResponse;
import ac.jwooo.eye_on.domain.agent.application.dto.response.AgentConfigResponse;
import ac.jwooo.eye_on.domain.agent.application.usecase.ChatWithAgentUseCase;
import ac.jwooo.eye_on.domain.agent.application.usecase.GetAgentConfigUseCase;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final GetAgentConfigUseCase getAgentConfigUseCase;
    private final ChatWithAgentUseCase chatWithAgentUseCase;

    @GetMapping("/config")
    public AgentConfigResponse getConfig(Authentication authentication) {
        return getAgentConfigUseCase.execute(extractUserId(authentication));
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(
            Authentication authentication,
            @Valid @RequestBody AgentChatRequest request
    ) {
        return chatWithAgentUseCase.execute(extractUserId(authentication), request);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
