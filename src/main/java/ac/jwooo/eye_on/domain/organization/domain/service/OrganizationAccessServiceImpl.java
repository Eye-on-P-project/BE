package ac.jwooo.eye_on.domain.organization.domain.service;

import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationRepository;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationAccessServiceImpl implements OrganizationAccessService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public Organization validateAdminAccess(Long requesterUserId, Long organizationId) {
        User requester = getAdminRequester(requesterUserId);
        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));

        String requesterOrganization = normalizeOrganization(requester.getOrganization());
        if (!organization.getCode().equalsIgnoreCase(requesterOrganization)) {
            throw new CustomException(ErrorCode.ORGANIZATION_ACCESS_DENIED);
        }

        return organization;
    }

    @Override
    public Organization resolveOwnedOrganization(Long requesterUserId) {
        User requester = getAdminRequester(requesterUserId);
        String requesterOrganization = normalizeOrganization(requester.getOrganization());

        return organizationRepository.findByCodeAndDeletedAtIsNull(requesterOrganization)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));
    }

    private User getAdminRequester(Long requesterUserId) {
        User requester = userRepository.findByIdAndDeletedAtIsNull(requesterUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (requester.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.ORGANIZATION_ADMIN_REQUIRED);
        }
        return requester;
    }

    private String normalizeOrganization(String organization) {
        if (!StringUtils.hasText(organization)) {
            throw new CustomException(ErrorCode.ORGANIZATION_ACCESS_DENIED);
        }
        return organization.trim().toUpperCase();
    }
}
