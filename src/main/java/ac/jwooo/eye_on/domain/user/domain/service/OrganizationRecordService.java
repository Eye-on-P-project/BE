package ac.jwooo.eye_on.domain.user.domain.service;

import java.util.List;

import ac.jwooo.eye_on.domain.user.application.dto.request.CreateOrganizationRecordRequest;
import ac.jwooo.eye_on.domain.user.application.dto.response.OrganizationRecordResponse;

public interface OrganizationRecordService {

    OrganizationRecordResponse createOrganizationRecord(CreateOrganizationRecordRequest request);

    void deleteOrganizationRecord(Long organizationRecordId);

    List<OrganizationRecordResponse> getAllOrganizationRecords();
}
