package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface OrganizationSessionCountProjection {

    Long getOrganizationId();

    Long getSessionCount();
}
