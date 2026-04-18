package ac.jwooo.eye_on.domain.organization.domain.service;

import java.util.List;

import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;

public interface OrganizationMemberService {

    OrganizationMember addMember(Long organizationId, Long userId);

    List<OrganizationMember> getMembers(Long organizationId);

    void removeMember(Long organizationId, Long memberId);
}
