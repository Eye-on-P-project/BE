package ac.jwooo.eye_on.domain.user.application.dto.response;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationCode;
import com.fasterxml.jackson.annotation.JsonFormat;

public record OrganizationRecordResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long id,
        String code,
        String description,
        LocalDateTime createdAt
) {
    public static OrganizationRecordResponse from(OrganizationCode organizationCode) {
        return new OrganizationRecordResponse(
                organizationCode.getId(),
                organizationCode.getCode(),
                organizationCode.getDescription(),
                organizationCode.getCreatedAt()
        );
    }
}
