package ac.jwooo.eye_on.domain.auth.application.dto.response;

import ac.jwooo.eye_on.domain.auth.domain.service.AuthResult;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

public record AuthTokenResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
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

