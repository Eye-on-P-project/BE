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
        name = "org_user_daily_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_org_user_daily_stats_org_user_date",
                        columnNames = {"organization_id", "user_id", "stat_date"}
                )
        },
        indexes = {
                @Index(name = "idx_org_user_daily_stats_org_date", columnList = "organization_id,stat_date,deleted_at"),
                @Index(name = "idx_org_user_daily_stats_org_user", columnList = "organization_id,user_id,deleted_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationUserDailyRiskStat extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "drowsy_count", nullable = false)
    private Integer drowsyCount;

    @Column(name = "sleep_count", nullable = false)
    private Integer sleepCount;

    @Column(name = "total_risk_count", nullable = false)
    private Integer totalRiskCount;

    @Builder(access = AccessLevel.PRIVATE)
    private OrganizationUserDailyRiskStat(
            Long organizationId,
            Long userId,
            LocalDate statDate,
            Integer drowsyCount,
            Integer sleepCount,
            Integer totalRiskCount
    ) {
        this.organizationId = organizationId;
        this.userId = userId;
        this.statDate = statDate;
        this.drowsyCount = drowsyCount;
        this.sleepCount = sleepCount;
        this.totalRiskCount = totalRiskCount;
    }

    public static OrganizationUserDailyRiskStat create(
            Long organizationId,
            Long userId,
            LocalDate statDate,
            long drowsyCount,
            long sleepCount,
            long totalRiskCount
    ) {
        return OrganizationUserDailyRiskStat.builder()
                .organizationId(organizationId)
                .userId(userId)
                .statDate(statDate)
                .drowsyCount(toSafeInt(drowsyCount))
                .sleepCount(toSafeInt(sleepCount))
                .totalRiskCount(toSafeInt(totalRiskCount))
                .build();
    }

    private static int toSafeInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
