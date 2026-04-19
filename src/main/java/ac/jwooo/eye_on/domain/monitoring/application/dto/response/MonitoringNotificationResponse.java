package ac.jwooo.eye_on.domain.monitoring.application.dto.response;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.Notification;
import ac.jwooo.eye_on.domain.monitoring.domain.entity.NotificationType;
import ac.jwooo.eye_on.domain.monitoring.domain.repository.NotificationFeedProjection;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MonitoringNotificationResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long notificationId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long userId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long targetUserId,
        String userName,
        NotificationType type,
        String content,
        LocalDateTime occurredAt
) {
    public static MonitoringNotificationResponse fromProjection(NotificationFeedProjection projection) {
        return new MonitoringNotificationResponse(
                projection.getNotificationId(),
                projection.getUserId(),
                projection.getTargetUserId(),
                projection.getUserName(),
                NotificationType.valueOf(projection.getType()),
                projection.getContent(),
                projection.getOccurredAt()
        );
    }

    public static MonitoringNotificationResponse fromEntity(
            Notification notification,
            String userName,
            LocalDateTime occurredAt
    ) {
        return new MonitoringNotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getTargetUserId(),
                userName,
                notification.getType(),
                notification.getContent(),
                occurredAt
        );
    }
}
