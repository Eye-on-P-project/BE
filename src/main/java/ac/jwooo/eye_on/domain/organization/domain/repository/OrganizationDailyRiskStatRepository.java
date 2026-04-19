package ac.jwooo.eye_on.domain.organization.domain.repository;

import java.time.LocalDate;
import java.util.List;

import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationDailyRiskStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrganizationDailyRiskStatRepository extends JpaRepository<OrganizationDailyRiskStat, Long> {

    List<OrganizationDailyRiskStat> findAllByOrganizationIdAndStatDateBetweenAndDeletedAtIsNullOrderByStatDateAsc(
            Long organizationId,
            LocalDate from,
            LocalDate to
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM OrganizationDailyRiskStat s WHERE s.statDate = :statDate")
    void deleteAllByStatDate(@Param("statDate") LocalDate statDate);
}
