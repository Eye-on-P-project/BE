package ac.jwooo.eye_on.domain.organization.domain.service;

import ac.jwooo.eye_on.domain.user.domain.entity.Organization;

public interface OrganizationAccessService {

    Organization validateAdminAccess(Long requesterUserId, Long organizationId);

    Organization resolveOwnedOrganization(Long requesterUserId);
}
