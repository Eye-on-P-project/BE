package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.time.LocalDateTime;

public interface NotificationFeedProjection {

    Long getNotificationId();

    Long getUserId();

    Long getTargetUserId();

    String getUserName();

    String getType();

    String getContent();

    LocalDateTime getOccurredAt();
}
