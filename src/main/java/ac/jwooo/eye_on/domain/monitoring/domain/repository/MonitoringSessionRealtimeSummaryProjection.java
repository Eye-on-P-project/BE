package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface MonitoringSessionRealtimeSummaryProjection {

    Long getTotalMemberCount();

    Long getActiveSessionCount();

    Long getWarningSessionCount();

    Long getDrowsyWarningSessionCount();

    Long getSleepWarningSessionCount();
}
