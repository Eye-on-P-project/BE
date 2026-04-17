package ac.jwooo.eye_on.domain.user.domain.service;

import java.util.List;

import ac.jwooo.eye_on.domain.user.application.dto.request.CreateOrganizationRecordRequest;
import ac.jwooo.eye_on.domain.user.application.dto.response.OrganizationRecordResponse;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationCode;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationCodeRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationRecordServiceImpl implements OrganizationRecordService {

    private final OrganizationCodeRepository organizationCodeRepository;

    @Override
    @Transactional
    public OrganizationRecordResponse createOrganizationRecord(CreateOrganizationRecordRequest request) {
        String normalizedCode = normalizeCode(request.code());
        if (organizationCodeRepository.existsByCodeAndDeletedAtIsNull(normalizedCode)) {
            throw new CustomException(ErrorCode.ORGANIZATION_CODE_ALREADY_EXISTS);
        }

        OrganizationCode organizationCode = OrganizationCode.builder()
                .code(normalizedCode)
                .description(normalizeDescription(request.description()))
                .build();

        OrganizationCode savedOrganizationCode = organizationCodeRepository.save(organizationCode);
        return OrganizationRecordResponse.from(savedOrganizationCode);
    }

    @Override
    @Transactional
    public void deleteOrganizationRecord(Long organizationRecordId) {
        OrganizationCode organizationCode = organizationCodeRepository.findByIdAndDeletedAtIsNull(organizationRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_RECORD_NOT_FOUND));

        organizationCode.markDeleted();
    }

    @Override
    public List<OrganizationRecordResponse> getAllOrganizationRecords() {
        return organizationCodeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(OrganizationRecordResponse::from)
                .toList();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }
}
