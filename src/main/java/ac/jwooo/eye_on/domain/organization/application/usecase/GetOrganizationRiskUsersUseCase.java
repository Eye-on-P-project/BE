package ac.jwooo.eye_on.domain.organization.application.usecase;

import java.util.List;

import ac.jwooo.eye_on.domain.monitoring.domain.repository.MonitoringSessionRepository;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskUserResponse;
import ac.jwooo.eye_on.domain.organization.domain.service.OrganizationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetOrganizationRiskUsersUseCase {

    private final OrganizationAccessService organizationAccessService;
    private final MonitoringSessionRepository monitoringSessionRepository;

    @Transactional(readOnly = true)
    public List<OrganizationRiskUserResponse> execute(Long requesterUserId, Long organizationId) {
        organizationAccessService.validateAdminAccess(requesterUserId, organizationId);

        return monitoringSessionRepository.findRiskUsersByOrganizationId(organizationId).stream()
                .map(OrganizationRiskUserResponse::from)
                .toList();
    }
}
