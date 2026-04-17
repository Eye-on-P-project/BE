package ac.example.eye.on.domain.user.repository;

import ac.example.eye.on.domain.user.entity.OrganizationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationCodeRepository extends JpaRepository<OrganizationCode, Long> {

    boolean existsByCodeAndDeletedAtIsNull(String code);
}

