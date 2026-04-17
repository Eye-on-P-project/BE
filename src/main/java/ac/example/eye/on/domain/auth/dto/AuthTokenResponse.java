package ac.example.eye.on.domain.auth.dto;

import ac.example.eye.on.domain.auth.service.AuthResult;
import ac.example.eye.on.domain.user.entity.UserRole;

public record AuthTokenResponse(
        Long userId,
        String accessToken,
        String refreshToken,
        UserRole role
) {
    public static AuthTokenResponse from(AuthResult authResult, String refreshToken) {
        return new AuthTokenResponse(
                authResult.userId(),
                authResult.accessToken(),
                refreshToken,
                authResult.role()
        );
    }
}

