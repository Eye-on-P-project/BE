package ac.jwooo.eye_on.domain.organization.ui;

import ac.jwooo.eye_on.domain.organization.application.dto.request.RejectOrganizationSignupRequest;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationSignupReviewResponse;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationSignupReviewService;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationStatus;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-admin/organizations/signups")
@RequiredArgsConstructor
public class SystemAdminOrganizationController {

    private final OrganizationSignupReviewService organizationSignupReviewService;

    @GetMapping
    public List<OrganizationSignupReviewResponse> listSignups(
            Authentication authentication,
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) String query
    ) {
        return organizationSignupReviewService.listSignups(extractUserId(authentication), status, query);
    }

    @GetMapping("/{organizationId}")
    public OrganizationSignupReviewResponse getSignup(
            Authentication authentication,
            @PathVariable Long organizationId
    ) {
        return organizationSignupReviewService.getSignup(extractUserId(authentication), organizationId);
    }

    @PatchMapping("/{organizationId}/approve")
    public Map<String, Object> approve(
            Authentication authentication,
            @PathVariable Long organizationId
    ) {
        organizationSignupReviewService.approve(extractUserId(authentication), organizationId);
        return Map.of("success", true);
    }

    @PatchMapping("/{organizationId}/reject")
    public Map<String, Object> reject(
            Authentication authentication,
            @PathVariable Long organizationId,
            @Valid @RequestBody RejectOrganizationSignupRequest request
    ) {
        organizationSignupReviewService.reject(extractUserId(authentication), organizationId, request);
        return Map.of("success", true);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
