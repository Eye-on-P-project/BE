package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitoringEventLogRepository extends JpaRepository<MonitoringEventLog, Long> {

    Optional<MonitoringEventLog> findTopBySessionIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long sessionId);

    @Query(
            value = """
                    SELECT
                        YEAR(mel.created_at) AS year,
                        MONTH(mel.created_at) AS month,
                        DAY(mel.created_at) AS day,
                        HOUR(mel.created_at) AS hour,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'DROWSY' THEN 1 ELSE 0 END), 0) AS drowsyCount,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'SLEEP' THEN 1 ELSE 0 END), 0) AS sleepCount,
                        COUNT(*) AS totalRiskCount
                    FROM monitoring_event_logs mel
                    JOIN monitoring_sessions ms
                      ON ms.id = mel.session_id
                     AND ms.deleted_at IS NULL
                     AND ms.mode = 'ORGANIZATION'
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    WHERE mel.deleted_at IS NULL
                      AND mel.event_type IN ('DROWSY', 'SLEEP')
                      AND mel.created_at >= :rangeStart
                      AND mel.created_at < :rangeEndExclusive
                    GROUP BY YEAR(mel.created_at), MONTH(mel.created_at), DAY(mel.created_at), HOUR(mel.created_at)
                    ORDER BY year ASC, month ASC, day ASC, hour ASC
                    """,
            nativeQuery = true
    )
    List<TimeBucketRiskCountProjection> findHourlyRiskCountsByOrganizationAndRange(
            @Param("organizationId") Long organizationId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );

    @Query(
            value = """
                    SELECT
                        m.organization_id AS organizationId,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'DROWSY' THEN 1 ELSE 0 END), 0) AS drowsyCount,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'SLEEP' THEN 1 ELSE 0 END), 0) AS sleepCount,
                        COUNT(*) AS totalRiskCount
                    FROM monitoring_event_logs mel
                    JOIN monitoring_sessions ms
                      ON ms.id = mel.session_id
                     AND ms.deleted_at IS NULL
                     AND ms.mode = 'ORGANIZATION'
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.deleted_at IS NULL
                    WHERE mel.deleted_at IS NULL
                      AND mel.event_type IN ('DROWSY', 'SLEEP')
                      AND mel.created_at >= :rangeStart
                      AND mel.created_at < :rangeEndExclusive
                    GROUP BY m.organization_id
                    """,
            nativeQuery = true
    )
    List<OrganizationRiskCountProjection> findOrganizationRiskCountsByRange(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );

    @Query(
            value = """
                    SELECT
                        m.organization_id AS organizationId,
                        ms.user_id AS userId,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'DROWSY' THEN 1 ELSE 0 END), 0) AS drowsyCount,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'SLEEP' THEN 1 ELSE 0 END), 0) AS sleepCount,
                        COUNT(*) AS totalRiskCount
                    FROM monitoring_event_logs mel
                    JOIN monitoring_sessions ms
                      ON ms.id = mel.session_id
                     AND ms.deleted_at IS NULL
                     AND ms.mode = 'ORGANIZATION'
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.deleted_at IS NULL
                    WHERE mel.deleted_at IS NULL
                      AND mel.event_type IN ('DROWSY', 'SLEEP')
                      AND mel.created_at >= :rangeStart
                      AND mel.created_at < :rangeEndExclusive
                    GROUP BY m.organization_id, ms.user_id
                    """,
            nativeQuery = true
    )
    List<OrganizationUserRiskCountProjection> findOrganizationUserRiskCountsByRange(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );

    @Query(
            value = """
                    SELECT
                        :organizationId AS organizationId,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'DROWSY' THEN 1 ELSE 0 END), 0) AS drowsyCount,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'SLEEP' THEN 1 ELSE 0 END), 0) AS sleepCount,
                        COUNT(*) AS totalRiskCount
                    FROM monitoring_event_logs mel
                    JOIN monitoring_sessions ms
                      ON ms.id = mel.session_id
                     AND ms.deleted_at IS NULL
                     AND ms.mode = 'ORGANIZATION'
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    WHERE mel.deleted_at IS NULL
                      AND mel.event_type IN ('DROWSY', 'SLEEP')
                      AND mel.created_at >= :rangeStart
                      AND mel.created_at < :rangeEndExclusive
                    """,
            nativeQuery = true
    )
    OrganizationRiskCountProjection findSingleOrganizationRiskCountByRange(
            @Param("organizationId") Long organizationId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );

    @Query(
            value = """
                    SELECT
                        m.organization_id AS organizationId,
                        ms.user_id AS userId,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'DROWSY' THEN 1 ELSE 0 END), 0) AS drowsyCount,
                        COALESCE(SUM(CASE WHEN mel.event_type = 'SLEEP' THEN 1 ELSE 0 END), 0) AS sleepCount,
                        COUNT(*) AS totalRiskCount
                    FROM monitoring_event_logs mel
                    JOIN monitoring_sessions ms
                      ON ms.id = mel.session_id
                     AND ms.deleted_at IS NULL
                     AND ms.mode = 'ORGANIZATION'
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    WHERE mel.deleted_at IS NULL
                      AND mel.event_type IN ('DROWSY', 'SLEEP')
                      AND mel.created_at >= :rangeStart
                      AND mel.created_at < :rangeEndExclusive
                    GROUP BY ms.user_id
                    """,
            nativeQuery = true
    )
    List<OrganizationUserRiskCountProjection> findUserRiskCountsByOrganizationAndRange(
            @Param("organizationId") Long organizationId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );
}
