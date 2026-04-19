package ac.jwooo.eye_on.domain.organization.ui;

import java.util.List;

import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskUserResponse;
import ac.jwooo.eye_on.domain.organization.application.usecase.GetOrganizationRiskUsersUseCase;
import ac.jwooo.eye_on.domain.organization.ui.spec.OrganizationRiskUserControllerSpec;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationRiskUserController implements OrganizationRiskUserControllerSpec {

    private final GetOrganizationRiskUsersUseCase getOrganizationRiskUsersUseCase;

    @Override
    @GetMapping("/{organizationId}/risk-users")
    public List<OrganizationRiskUserResponse> getRiskUsers(
            Authentication authentication,
            @PathVariable Long organizationId
    ) {
        return getOrganizationRiskUsersUseCase.execute(extractUserId(authentication), organizationId);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
