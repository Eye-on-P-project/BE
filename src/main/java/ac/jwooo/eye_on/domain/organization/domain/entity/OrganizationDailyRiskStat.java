package ac.jwooo.eye_on.domain.organization.domain.entity;

import java.time.LocalDate;

import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "org_daily_stats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_org_daily_stats_org_date", columnNames = {"organization_id", "stat_date"})
        },
        indexes = {
                @Index(name = "idx_org_daily_stats_org_date", columnList = "organization_id,stat_date,deleted_at"),
                @Index(name = "idx_org_daily_stats_date", columnList = "stat_date,deleted_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationDailyRiskStat extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "session_count", nullable = false)
    private Integer sessionCount;

    @Column(name = "drowsy_count", nullable = false)
    private Integer drowsyCount;

    @Column(name = "sleep_count", nullable = false)
    private Integer sleepCount;

    @Column(name = "total_risk_count", nullable = false)
    private Integer totalRiskCount;

    @Builder(access = AccessLevel.PRIVATE)
    private OrganizationDailyRiskStat(
            Long organizationId,
            LocalDate statDate,
            Integer sessionCount,
            Integer drowsyCount,
            Integer sleepCount,
            Integer totalRiskCount
    ) {
        this.organizationId = organizationId;
        this.statDate = statDate;
        this.sessionCount = sessionCount;
        this.drowsyCount = drowsyCount;
        this.sleepCount = sleepCount;
        this.totalRiskCount = totalRiskCount;
    }

    public static OrganizationDailyRiskStat create(
            Long organizationId,
            LocalDate statDate,
            long sessionCount,
            long drowsyCount,
            long sleepCount,
            long totalRiskCount
    ) {
        return OrganizationDailyRiskStat.builder()
                .organizationId(organizationId)
                .statDate(statDate)
                .sessionCount(toSafeInt(sessionCount))
                .drowsyCount(toSafeInt(drowsyCount))
                .sleepCount(toSafeInt(sleepCount))
                .totalRiskCount(toSafeInt(totalRiskCount))
                .build();
    }

    private static int toSafeInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
