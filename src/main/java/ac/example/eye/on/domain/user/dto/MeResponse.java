package ac.example.eye.on.domain.user.dto;

import ac.example.eye.on.domain.user.entity.Gender;
import ac.example.eye.on.domain.user.entity.UserRole;
import ac.example.eye.on.domain.user.entity.User;

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

