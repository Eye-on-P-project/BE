package ac.jwooo.eye_on.domain.monitoring.domain.entity;

import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "notification",
        indexes = {
                @Index(
                        name = "idx_notification_org_cursor",
                        columnList = "user_id,deleted_at,notification_id"
                ),
                @Index(
                        name = "idx_notification_target_cursor",
                        columnList = "target_user_id,deleted_at,notification_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "content", nullable = false, length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(
            Long userId,
            Long targetUserId,
            String content,
            NotificationType type
    ) {
        this.userId = userId;
        this.targetUserId = targetUserId;
        this.content = content;
        this.type = type;
    }

    public static Notification create(
            Long userId,
            Long targetUserId,
            String content,
            NotificationType type
    ) {
        return Notification.builder()
                .userId(userId)
                .targetUserId(targetUserId)
                .content(content)
                .type(type)
                .build();
    }
}
