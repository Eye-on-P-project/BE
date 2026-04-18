package ac.jwooo.eye_on.domain.user.application.dto.response;

import ac.jwooo.eye_on.domain.user.domain.entity.Gender;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.entity.User;

public record MeResponse(
        Long userId,
        String email,
        UserRole role,
        String organizationCode,
        String name,
        String nickname,
        Integer age,
        Gender gender
) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getOrganizationCode(),
                user.getName(),
                user.getNickname(),
                user.getAge(),
                user.getGender()
        );
    }
}

