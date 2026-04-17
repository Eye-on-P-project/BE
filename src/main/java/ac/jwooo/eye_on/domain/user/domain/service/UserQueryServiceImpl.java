package ac.jwooo.eye_on.domain.user.domain.service;

import ac.jwooo.eye_on.domain.user.application.dto.response.MeResponse;
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
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public MeResponse getMe(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MeResponse.from(user);
    }
}

