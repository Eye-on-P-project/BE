package ac.jwooo.eye_on.domain.organization.domain.entity;

import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "member",
        indexes = {
                @Index(name = "idx_member_org_active_user", columnList = "organization_id,deleted_at,user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationMember extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "member_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder(access = AccessLevel.PRIVATE)
    private OrganizationMember(Long organizationId, Long userId) {
        this.organizationId = organizationId;
        this.userId = userId;
    }

    public static OrganizationMember create(Long organizationId, Long userId) {
        return OrganizationMember.builder()
                .organizationId(organizationId)
                .userId(userId)
                .build();
    }
}
