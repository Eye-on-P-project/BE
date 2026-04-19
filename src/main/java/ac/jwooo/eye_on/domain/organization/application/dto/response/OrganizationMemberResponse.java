package ac.jwooo.eye_on.domain.organization.application.dto.response;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

public record OrganizationMemberResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long memberId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long organizationId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,

        String email,
        String name,
        String nickname,
        UserRole role,
        LocalDateTime createdAt
) {
    public static OrganizationMemberResponse from(OrganizationMember organizationMember, User user) {
        return new OrganizationMemberResponse(
                organizationMember.getId(),
                organizationMember.getOrganizationId(),
                organizationMember.getUserId(),
                user != null ? user.getEmail() : null,
                user != null ? user.getName() : null,
                user != null ? user.getNickname() : null,
                user != null ? user.getRole() : null,
                organizationMember.getCreatedAt()
        );
    }
}
