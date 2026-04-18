package ac.jwooo.eye_on.domain.organization.domain.repository;

import java.util.List;
import java.util.Optional;

import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    boolean existsByOrganizationIdAndUserIdAndDeletedAtIsNull(Long organizationId, Long userId);

    List<OrganizationMember> findAllByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long organizationId);

    Optional<OrganizationMember> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);
}
