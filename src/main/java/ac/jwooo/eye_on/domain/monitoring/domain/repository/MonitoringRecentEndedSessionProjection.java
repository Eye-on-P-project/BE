package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.time.LocalDateTime;

public interface MonitoringRecentEndedSessionProjection {

    Long getSessionId();

    Long getUserId();

    String getUserName();

    LocalDateTime getStartedAtApp();

    LocalDateTime getEndedAtApp();

    Integer getDurationMinutes();

    Long getDrowsyCount();

    Long getSleepCount();

    Long getTotalRiskCount();
}
