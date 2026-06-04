package ac.jwooo.eye_on.domain.user.domain.service;

import ac.jwooo.eye_on.domain.user.application.dto.request.ChangePasswordRequest;
import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationRepository;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPasswordServiceImpl implements UserPasswordService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long userOrganizationId = user.getOrganization();
        if (userOrganizationId == null) {
            throw new CustomException(ErrorCode.ORGANIZATION_CODE_MISMATCH);
        }
        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(userOrganizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));

        String requestOrganizationCode = normalizeOrganizationCode(request.organization());
        String userOrganizationCode = normalizeOrganizationCode(organization.getCode());
        if (!StringUtils.hasText(requestOrganizationCode) || !requestOrganizationCode.equals(userOrganizationCode)) {
            throw new CustomException(ErrorCode.ORGANIZATION_CODE_MISMATCH);
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    private String normalizeOrganizationCode(String organizationCode) {
        return organizationCode == null ? null : organizationCode.trim().toUpperCase();
    }
}
