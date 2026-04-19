package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface OrganizationUserRiskCountProjection {

    Long getOrganizationId();

    Long getUserId();

    Long getDrowsyCount();

    Long getSleepCount();

    Long getTotalRiskCount();
}
