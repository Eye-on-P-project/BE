package ac.jwooo.eye_on.domain.organization.ui;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.organization.application.dto.request.AddOrganizationMemberRequest;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationMemberResponse;
import ac.jwooo.eye_on.domain.organization.application.usecase.AddOrganizationMemberUseCase;
import ac.jwooo.eye_on.domain.organization.application.usecase.GetOrganizationMembersUseCase;
import ac.jwooo.eye_on.domain.organization.application.usecase.RemoveOrganizationMemberUseCase;
import ac.jwooo.eye_on.domain.organization.ui.spec.OrganizationMemberControllerSpec;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/members")
@RequiredArgsConstructor
public class OrganizationMemberController implements OrganizationMemberControllerSpec {

    private final AddOrganizationMemberUseCase addOrganizationMemberUseCase;
    private final GetOrganizationMembersUseCase getOrganizationMembersUseCase;
    private final RemoveOrganizationMemberUseCase removeOrganizationMemberUseCase;

    @Override
    @PostMapping
    public OrganizationMemberResponse addMember(
            Authentication authentication,
            @Valid @RequestBody AddOrganizationMemberRequest request
    ) {
        return addOrganizationMemberUseCase.execute(extractUserId(authentication), request);
    }

    @Override
    @GetMapping
    public List<OrganizationMemberResponse> getMembers(Authentication authentication) {
        return getOrganizationMembersUseCase.execute(extractUserId(authentication));
    }

    @Override
    @DeleteMapping("/{memberId}")
    public Map<String, Object> removeMember(
            Authentication authentication,
            @PathVariable Long memberId
    ) {
        removeOrganizationMemberUseCase.execute(extractUserId(authentication), memberId);
        return Map.of("success", true);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
