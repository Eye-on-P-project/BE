package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface OrganizationRiskCountProjection {

    Long getOrganizationId();

    Long getDrowsyCount();

    Long getSleepCount();

    Long getTotalRiskCount();
}
