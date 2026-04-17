package ac.example.eye.on.global.security;

import ac.example.eye.on.domain.auth.model.ClientType;
import ac.example.eye.on.domain.user.entity.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole role,
        ClientType clientType
) {
}

