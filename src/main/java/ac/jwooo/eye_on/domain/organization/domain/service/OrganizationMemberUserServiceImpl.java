package ac.jwooo.eye_on.domain.organization.domain.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationMemberUserServiceImpl implements OrganizationMemberUserService {

    private final UserRepository userRepository;

    @Override
    public User getActiveUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(email))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public Map<Long, User> getActiveUsersByIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllByIdInAndDeletedAtIsNull(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
