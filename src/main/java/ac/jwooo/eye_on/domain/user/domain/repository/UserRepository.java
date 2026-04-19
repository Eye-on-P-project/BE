package ac.jwooo.eye_on.domain.user.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    List<User> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    boolean existsByOrganizationCodeAndRoleAndDeletedAtIsNull(String organizationCode, UserRole role);
}
