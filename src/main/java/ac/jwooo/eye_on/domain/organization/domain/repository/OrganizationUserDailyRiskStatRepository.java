package ac.jwooo.eye_on.domain.organization.domain.repository;

import java.time.LocalDate;
import java.util.List;

import ac.jwooo.eye_on.domain.organization.domain.entity.OrganizationUserDailyRiskStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrganizationUserDailyRiskStatRepository extends JpaRepository<OrganizationUserDailyRiskStat, Long> {

    @Query(
            value = """
                    SELECT
                        s.user_id AS userId,
                        u.name AS name,
                        COALESCE(SUM(s.total_risk_count), 0) AS totalRiskCount
                    FROM org_user_daily_stats s
                    JOIN users u
                      ON u.id = s.user_id
                     AND u.deleted_at IS NULL
                    WHERE s.organization_id = :organizationId
                      AND s.deleted_at IS NULL
                      AND s.stat_date >= :from
                      AND s.stat_date <= :to
                    GROUP BY s.user_id, u.name
                    """,
            nativeQuery = true
    )
    List<OrganizationTopRiskUserProjection> findUserRiskTotalsByOrganizationAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM OrganizationUserDailyRiskStat s WHERE s.statDate = :statDate")
    void deleteAllByStatDate(@Param("statDate") LocalDate statDate);
}
