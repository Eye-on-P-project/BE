package ac.example.eye.on.domain.user.service;

import ac.example.eye.on.domain.user.dto.MeResponse;
import ac.example.eye.on.domain.user.entity.User;
import ac.example.eye.on.domain.user.repository.UserRepository;
import ac.example.eye.on.global.exception.CustomException;
import ac.example.eye.on.global.exception.ErrorCode;
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

