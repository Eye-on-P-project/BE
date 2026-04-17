package ac.example.eye.on.domain.auth.service;

import ac.example.eye.on.domain.user.entity.UserRole;

public record AuthResult(
        Long userId,
        String accessToken,
        String refreshToken,
        UserRole role
) {
}

