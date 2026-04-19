package ac.jwooo.eye_on.domain.organization.ui;

import java.time.LocalDate;
import java.util.List;

import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskStatsResponse;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskUserResponse;
import ac.jwooo.eye_on.domain.organization.application.usecase.GetOrganizationRiskStatsUseCase;
import ac.jwooo.eye_on.domain.organization.application.usecase.GetOrganizationRiskUsersUseCase;
import ac.jwooo.eye_on.domain.organization.ui.spec.OrganizationRiskUserControllerSpec;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationRiskUserController implements OrganizationRiskUserControllerSpec {

    private final GetOrganizationRiskUsersUseCase getOrganizationRiskUsersUseCase;
    private final GetOrganizationRiskStatsUseCase getOrganizationRiskStatsUseCase;

    @Override
    @GetMapping("/{organizationId}/risk-users")
    public List<OrganizationRiskUserResponse> getRiskUsers(
            Authentication authentication,
            @PathVariable Long organizationId
    ) {
        return getOrganizationRiskUsersUseCase.execute(extractUserId(authentication), organizationId);
    }

    @Override
    @GetMapping("/{organizationId}/analysis/risk-stats")
    public OrganizationRiskStatsResponse getRiskStats(
            Authentication authentication,
            @PathVariable Long organizationId,
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return getOrganizationRiskStatsUseCase.execute(
                extractUserId(authentication),
                organizationId,
                granularity,
                from,
                to
        );
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
