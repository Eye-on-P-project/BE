package ac.jwooo.eye_on.domain.user.domain.repository;

import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationCodeRepository extends JpaRepository<OrganizationCode, Long> {

    boolean existsByCodeAndDeletedAtIsNull(String code);

    Optional<OrganizationCode> findByIdAndDeletedAtIsNull(Long id);

    List<OrganizationCode> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
