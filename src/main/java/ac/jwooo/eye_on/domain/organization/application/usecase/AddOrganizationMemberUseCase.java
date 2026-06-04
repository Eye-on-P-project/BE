package ac.jwooo.eye_on.domain.organization.application.usecase;

import ac.jwooo.eye_on.domain.organization.application.dto.request.AddOrganizationMemberRequest;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationMemberResponse;
import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationMemberService;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationMemberUserService;
import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AddOrganizationMemberUseCase {

    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMemberService organizationMemberService;
    private final OrganizationMemberUserService organizationMemberUserService;

    @Transactional
    public OrganizationMemberResponse execute(
            Long requesterUserId,
            AddOrganizationMemberRequest request
    ) {
        Organization ownedOrganization = organizationAccessService.resolveOwnedOrganization(requesterUserId);
        Long organizationId = ownedOrganization.getId();

        User targetUser = organizationMemberUserService.getActiveUserByEmail(request.email());
        if (targetUser.getRole() == UserRole.ADMIN || targetUser.getRole() == UserRole.SYSTEM_ADMIN) {
            throw new CustomException(ErrorCode.ORGANIZATION_MEMBER_ADMIN_NOT_ALLOWED);
        }

        OrganizationMember createdMember = organizationMemberService.addMember(organizationId, targetUser.getId());
        return OrganizationMemberResponse.from(createdMember, targetUser);
    }
}
