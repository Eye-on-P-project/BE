package ac.jwooo.eye_on.domain.auth.domain.service;

import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;

public record AuthResult(
        Long userId,
        String accessToken,
        String refreshToken,
        UserRole role
) {
}

