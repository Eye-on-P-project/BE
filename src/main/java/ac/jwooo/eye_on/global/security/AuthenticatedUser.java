package ac.jwooo.eye_on.global.security;

import ac.jwooo.eye_on.domain.auth.domain.entity.ClientType;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole role,
        ClientType clientType
) {
}

