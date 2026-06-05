package ac.jwooo.eye_on.domain.user.application.dto.response;

import ac.jwooo.eye_on.domain.user.domain.entity.Gender;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.entity.UserSubscription;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MeResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,
        String email,
        UserRole role,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long organization,
        UserSubscription subscription,
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
                user.getOrganization(),
                user.getSubscription(),
                user.getName(),
                user.getNickname(),
                user.getAge(),
                user.getGender()
        );
    }
}
