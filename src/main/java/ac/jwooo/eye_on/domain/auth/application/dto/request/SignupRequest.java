package ac.jwooo.eye_on.domain.auth.application.dto.request;

import ac.jwooo.eye_on.domain.user.domain.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 72, message = "비밀번호는 4자 이상 72자 이하여야 합니다.")
        String password,

        String organizationCode,
        String name,
        String nickname,

        @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
        @Max(value = 120, message = "나이는 120 이하여야 합니다.")
        Integer age,
        Gender gender
) {
}

