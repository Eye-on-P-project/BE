package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.util.List;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(
            value = """
                    SELECT
                        n.notification_id AS notificationId,
                        n.user_id AS userId,
                        n.target_user_id AS targetUserId,
                        COALESCE(
                            NULLIF(TRIM(u.name), ''),
                            NULLIF(TRIM(u.nickname), ''),
                            u.email,
                            CONCAT('사용자 ', n.user_id)
                        ) AS userName,
                        n.type AS type,
                        n.content AS content,
                        n.created_at AS occurredAt
                    FROM notification n
                    JOIN member m
                      ON m.user_id = n.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    JOIN users u
                      ON u.id = n.user_id
                     AND u.deleted_at IS NULL
                    WHERE n.deleted_at IS NULL
                      AND (:cursor IS NULL OR n.notification_id < :cursor)
                    ORDER BY n.notification_id DESC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<NotificationFeedProjection> findRecentByOrganizationIdWithCursor(
            @Param("organizationId") Long organizationId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit
    );
}
