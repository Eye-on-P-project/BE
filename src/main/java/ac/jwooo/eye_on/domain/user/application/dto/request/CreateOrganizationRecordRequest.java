package ac.jwooo.eye_on.domain.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRecordRequest(
        @NotBlank(message = "조직 코드는 필수입니다.")
        @Size(max = 100, message = "조직 코드는 100자 이하여야 합니다.")
        String code,

        @Size(max = 255, message = "설명은 255자 이하여야 합니다.")
        String description
) {
}
