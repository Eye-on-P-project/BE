package ac.jwooo.eye_on.domain.organization.application.usecase;

import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RemoveOrganizationMemberUseCase {

    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMemberService organizationMemberService;

    @Transactional
    public void execute(Long requesterUserId, Long memberId) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(requesterUserId).getId();
        organizationMemberService.removeMember(organizationId, memberId);
    }
}
