package ac.jwooo.eye_on.domain.organization.domain.service;

import ac.jwooo.eye_on.domain.organization.application.dto.request.RejectOrganizationSignupRequest;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationSignupReviewResponse;
import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationStatus;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationRepository;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationSignupReviewService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public List<OrganizationSignupReviewResponse> listSignups(
            Long reviewerUserId,
            OrganizationStatus status,
            String query
    ) {
        validateSystemAdmin(reviewerUserId);

        String normalizedQuery = normalizeQuery(query);
        List<Organization> organizations = organizationRepository.searchForReview(status, normalizedQuery);
        if (organizations.isEmpty()) {
            return List.of();
        }

        List<Long> organizationIds = organizations.stream()
                .map(Organization::getId)
                .toList();
        Map<Long, String> emailByOrganization = userRepository
                .findAllByOrganizationInAndRoleAndDeletedAtIsNull(organizationIds, UserRole.ADMIN)
                .stream()
                .collect(Collectors.toMap(
                        User::getOrganization,
                        User::getEmail,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));

        return organizations.stream()
                .map(organization -> OrganizationSignupReviewResponse.from(
                        organization,
                        emailByOrganization.get(organization.getId())
                ))
                .toList();
    }

    public OrganizationSignupReviewResponse getSignup(Long reviewerUserId, Long organizationId) {
        validateSystemAdmin(reviewerUserId);
        Organization organization = getOrganization(organizationId);
        String requesterEmail = userRepository
                .findFirstByOrganizationAndRoleAndDeletedAtIsNull(organization.getId(), UserRole.ADMIN)
                .map(User::getEmail)
                .orElse(null);
        return OrganizationSignupReviewResponse.from(organization, requesterEmail);
    }

    @Transactional
    public void approve(Long reviewerUserId, Long organizationId) {
        validateSystemAdmin(reviewerUserId);
        Organization organization = getOrganization(organizationId);
        if (organization.getStatus() != OrganizationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "승인 가능한 상태(PENDING)가 아닙니다.");
        }
        organization.approve();
    }

    @Transactional
    public void reject(
            Long reviewerUserId,
            Long organizationId,
            RejectOrganizationSignupRequest request
    ) {
        validateSystemAdmin(reviewerUserId);
        Organization organization = getOrganization(organizationId);
        if (organization.getStatus() != OrganizationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "거절 가능한 상태(PENDING)가 아닙니다.");
        }

        String reasonCodes = request.reasonCodes() == null || request.reasonCodes().isEmpty()
                ? null
                : request.reasonCodes().stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .collect(Collectors.joining(","));
        organization.reject(reasonCodes, request.reasonDetail());
    }

    private Organization getOrganization(Long organizationId) {
        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));
    }

    private void validateSystemAdmin(Long reviewerUserId) {
        User reviewer = userRepository.findByIdAndDeletedAtIsNull(reviewerUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (reviewer.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new CustomException(ErrorCode.SYSTEM_ADMIN_REQUIRED);
        }
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }
}
