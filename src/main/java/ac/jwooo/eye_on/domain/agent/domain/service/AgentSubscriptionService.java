package ac.jwooo.eye_on.domain.agent.domain.service;

import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationSubscription;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserSubscription;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationRepository;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentSubscriptionService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public boolean canUseAgent(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getSubscription() != UserSubscription.FREE) {
            return true;
        }

        Long organizationId = user.getOrganization();
        if (organizationId == null) {
            return false;
        }

        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .map(organization -> organization.getSubscription() != OrganizationSubscription.FREE)
                .orElse(false);
    }
}
