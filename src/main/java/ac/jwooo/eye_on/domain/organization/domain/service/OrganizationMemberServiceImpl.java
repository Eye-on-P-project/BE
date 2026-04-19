package ac.jwooo.eye_on.domain.organization.domain.service;

import java.util.List;

import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;
import ac.jwooo.eye_on.domain.organization.domain.repository.OrganizationMemberRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private final OrganizationMemberRepository organizationMemberRepository;

    @Override
    @Transactional
    public OrganizationMember addMember(Long organizationId, Long userId) {
        if (organizationMemberRepository.existsByOrganizationIdAndUserIdAndDeletedAtIsNull(organizationId, userId)) {
            throw new CustomException(ErrorCode.ORGANIZATION_MEMBER_ALREADY_EXISTS);
        }

        OrganizationMember organizationMember = OrganizationMember.create(organizationId, userId);
        return organizationMemberRepository.save(organizationMember);
    }

    @Override
    public List<OrganizationMember> getMembers(Long organizationId) {
        return organizationMemberRepository.findAllByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId);
    }

    @Override
    @Transactional
    public void removeMember(Long organizationId, Long memberId) {
        OrganizationMember organizationMember = organizationMemberRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(memberId, organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_MEMBER_NOT_FOUND));

        organizationMember.markDeleted();
    }
}
