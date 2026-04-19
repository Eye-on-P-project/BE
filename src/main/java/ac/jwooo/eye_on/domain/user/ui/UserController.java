package ac.jwooo.eye_on.domain.user.ui;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.user.application.dto.request.CreateOrganizationRecordRequest;
import ac.jwooo.eye_on.domain.user.application.dto.response.MeResponse;
import ac.jwooo.eye_on.domain.user.application.dto.response.OrganizationRecordResponse;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.service.OrganizationRecordService;
import ac.jwooo.eye_on.domain.user.domain.service.UserQueryService;
import ac.jwooo.eye_on.domain.user.ui.spec.UserControllerSpec;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

    private final UserQueryService userQueryService;
    private final OrganizationRecordService organizationRecordService;

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userQueryService.getMe(principal.userId());
    }

    @PostMapping("/dev/organizations")
    public OrganizationRecordResponse createOrganizationRecord(
            Authentication authentication,
            @Valid @RequestBody CreateOrganizationRecordRequest request
    ) {
        extractAdminUserId(authentication);
        return organizationRecordService.createOrganizationRecord(request);
    }

    @GetMapping("/dev/organizations")
    public List<OrganizationRecordResponse> getOrganizationRecords(Authentication authentication) {
        extractAdminUserId(authentication);
        return organizationRecordService.getAllOrganizationRecords();
    }

    @DeleteMapping("/dev/organizations/{organizationRecordId}")
    public Map<String, Object> deleteOrganizationRecord(
            Authentication authentication,
            @PathVariable Long organizationRecordId
    ) {
        extractAdminUserId(authentication);
        organizationRecordService.deleteOrganizationRecord(organizationRecordId);
        return Map.of("success", true);
    }

    private Long extractAdminUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.role() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.ORGANIZATION_ADMIN_REQUIRED);
        }
        return principal.userId();
    }
}
