package ac.jwooo.eye_on.domain.user.domain.repository;

import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCorporateNumAndStatusInAndDeletedAtIsNull(
            String corporateNum,
            Collection<OrganizationStatus> statuses
    );

    Optional<Organization> findByIdAndDeletedAtIsNull(Long id);

    Optional<Organization> findByCodeAndDeletedAtIsNull(String code);

    @Query("""
            SELECT o
            FROM Organization o
            WHERE o.deletedAt IS NULL
              AND (:status IS NULL OR o.status = :status)
              AND (
                    :query IS NULL
                    OR LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(o.businessName) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(o.representativeName) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY o.createdAt DESC
            """)
    List<Organization> searchForReview(
            @Param("status") OrganizationStatus status,
            @Param("query") String query
    );
}
