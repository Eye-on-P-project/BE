package ac.jwooo.eye_on.domain.organization.domain.repository;

public interface OrganizationTopRiskUserProjection {

    Long getUserId();

    String getName();

    Long getTotalRiskCount();
}
