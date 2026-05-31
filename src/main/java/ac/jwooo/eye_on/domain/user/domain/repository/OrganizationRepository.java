package ac.jwooo.eye_on.domain.user.domain.repository;

import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByCodeAndDeletedAtIsNull(String code);

    Optional<Organization> findByIdAndDeletedAtIsNull(Long id);

    Optional<Organization> findByCodeAndDeletedAtIsNull(String code);
}
