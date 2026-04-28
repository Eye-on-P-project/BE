package ac.jwooo.eye_on.domain.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        @Size(min = 4, max = 72, message = "현재 비밀번호는 4자 이상 72자 이하여야 합니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 4, max = 72, message = "새 비밀번호는 4자 이상 72자 이하여야 합니다.")
        String newPassword
) {
}
