package ac.jwooo.eye_on.domain.organization.application.dto.response;

import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationStatus;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationSubscription;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrganizationSignupReviewResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long organizationId,
        String organizationCode,
        String organizationName,
        String businessName,
        String businessmanNum,
        LocalDate establishedAt,
        String representativeName,
        String coRepresentativeName,
        String corporateNum,
        String businessAddress,
        OrganizationStatus status,
        OrganizationSubscription subscription,
        String requesterEmail,
        String rejectReasonCodes,
        String rejectReasonDetail,
        LocalDateTime createdAt
) {
    public static OrganizationSignupReviewResponse from(Organization organization, String requesterEmail) {
        return new OrganizationSignupReviewResponse(
                organization.getId(),
                organization.getCode(),
                organization.getName(),
                organization.getBusinessName(),
                organization.getBusinessmanNum(),
                organization.getEstablishedAt(),
                organization.getRepresentativeName(),
                organization.getCoRepresentativeName(),
                organization.getCorporateNum(),
                organization.getBusinessAddress(),
                organization.getStatus(),
                organization.getSubscription(),
                requesterEmail,
                organization.getRejectReasonCodes(),
                organization.getRejectReasonDetail(),
                organization.getCreatedAt()
        );
    }
}
