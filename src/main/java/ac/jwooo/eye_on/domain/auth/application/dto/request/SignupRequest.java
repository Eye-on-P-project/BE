package ac.jwooo.eye_on.domain.auth.application.dto.request;

import ac.jwooo.eye_on.domain.user.domain.entity.Gender;
import java.time.LocalDate;
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

        String organization,

        @NotBlank(message = "조직(기업) 이름은 필수입니다.")
        @Size(max = 100, message = "조직(기업) 이름은 100자 이하여야 합니다.")
        String organizationName,

        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Size(max = 20, message = "사업자등록번호는 20자 이하여야 합니다.")
        String businessmanNum,

        LocalDate establishedAt,

        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Size(max = 100, message = "대표자 이름은 100자 이하여야 합니다.")
        String representativeName,

        @NotBlank(message = "법인등록번호는 필수입니다.")
        @Size(max = 20, message = "법인등록번호는 20자 이하여야 합니다.")
        String corporateNum,

        @NotBlank(message = "상호명은 필수입니다.")
        @Size(max = 200, message = "상호명은 200자 이하여야 합니다.")
        String businessName,

        @Size(max = 100, message = "공동대표자 이름은 100자 이하여야 합니다.")
        String coRepresentativeName,

        @Size(max = 255, message = "사업장 주소는 255자 이하여야 합니다.")
        String businessAddress,

        String name,
        String nickname,

        @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
        @Max(value = 120, message = "나이는 120 이하여야 합니다.")
        Integer age,
        Gender gender
) {
}
