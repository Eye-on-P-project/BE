package ac.jwooo.eye_on.domain.organization.application.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

public record RejectOrganizationSignupRequest(
        @Size(max = 10, message = "거절 사유 코드는 최대 10개까지 입력할 수 있습니다.")
        List<@Size(max = 50, message = "거절 사유 코드는 50자 이하여야 합니다.") String> reasonCodes,

        @Size(max = 500, message = "거절 상세 사유는 500자 이하여야 합니다.")
        String reasonDetail
) {
}
