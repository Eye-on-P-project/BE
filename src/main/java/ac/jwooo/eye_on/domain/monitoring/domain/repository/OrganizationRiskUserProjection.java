package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface OrganizationRiskUserProjection {

    Long getUserId();

    String getEmail();

    String getName();

    String getNickname();

    Long getTotalSessionCount();

    Long getDrowsyCount();

    Long getSleepCount();

    Long getTotalRiskCount();

    Integer getIsMonitoringActive();
}
