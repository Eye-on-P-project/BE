package ac.jwooo.eye_on.domain.organization.application.usecase;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationMemberResponse;
import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationMemberService;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationMemberUserService;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetOrganizationMembersUseCase {

    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMemberService organizationMemberService;
    private final OrganizationMemberUserService organizationMemberUserService;

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> execute(Long requesterUserId) {
        Long organizationId = organizationAccessService.resolveOwnedOrganization(requesterUserId).getId();

        List<OrganizationMember> organizationMembers = organizationMemberService.getMembers(organizationId);
        List<Long> userIds = organizationMembers.stream()
                .map(OrganizationMember::getUserId)
                .distinct()
                .toList();

        Map<Long, User> usersById = organizationMemberUserService.getActiveUsersByIds(userIds);
        return organizationMembers.stream()
                .map(member -> OrganizationMemberResponse.from(member, usersById.get(member.getUserId())))
                .toList();
    }
}
