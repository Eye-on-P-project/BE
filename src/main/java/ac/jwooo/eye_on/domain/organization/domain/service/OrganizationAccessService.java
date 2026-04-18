package ac.jwooo.eye_on.domain.organization.domain.service;

import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationCode;

public interface OrganizationAccessService {

    OrganizationCode validateAdminAccess(Long requesterUserId, Long organizationId);

    OrganizationCode resolveOwnedOrganization(Long requesterUserId);
}
