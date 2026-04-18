package ac.jwooo.eye_on.domain.organization.domain.service;

import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationCode;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationCodeRepository;
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
    private final OrganizationCodeRepository organizationCodeRepository;

    @Override
    public OrganizationCode validateAdminAccess(Long requesterUserId, Long organizationId) {
        User requester = getAdminRequester(requesterUserId);
        OrganizationCode organization = organizationCodeRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_RECORD_NOT_FOUND));

        String requesterOrganizationCode = normalizeOrganizationCode(requester.getOrganizationCode());
        if (!organization.getCode().equalsIgnoreCase(requesterOrganizationCode)) {
            throw new CustomException(ErrorCode.ORGANIZATION_ACCESS_DENIED);
        }

        return organization;
    }

    @Override
    public OrganizationCode resolveOwnedOrganization(Long requesterUserId) {
        User requester = getAdminRequester(requesterUserId);
        String requesterOrganizationCode = normalizeOrganizationCode(requester.getOrganizationCode());

        return organizationCodeRepository.findByCodeAndDeletedAtIsNull(requesterOrganizationCode)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_RECORD_NOT_FOUND));
    }

    private User getAdminRequester(Long requesterUserId) {
        User requester = userRepository.findByIdAndDeletedAtIsNull(requesterUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (requester.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.ORGANIZATION_ADMIN_REQUIRED);
        }
        return requester;
    }

    private String normalizeOrganizationCode(String organizationCode) {
        if (!StringUtils.hasText(organizationCode)) {
            throw new CustomException(ErrorCode.ORGANIZATION_ACCESS_DENIED);
        }
        return organizationCode.trim().toUpperCase();
    }
}
