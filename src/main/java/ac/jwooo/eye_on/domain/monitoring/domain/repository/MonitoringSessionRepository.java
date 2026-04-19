package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {

    Optional<MonitoringSession> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUserIdAndEndedAtServerIsNullAndDeletedAtIsNull(Long userId);

    List<MonitoringSession> findByUserIdAndEndedAtServerIsNullAndDeletedAtIsNull(Long userId);

    @Query(
            value = """
                    SELECT
                        (
                            SELECT COUNT(*)
                            FROM member m
                            WHERE m.organization_id = :organizationId
                              AND m.deleted_at IS NULL
                        ) AS totalMemberCount,
                        COUNT(ms.id) AS activeSessionCount,
                        COALESCE(SUM(
                                CASE
                                    WHEN latest_event.event_type IN ('DROWSY', 'SLEEP')
                                    THEN 1
                                    ELSE 0
                                END
                        ), 0)
                            AS warningSessionCount,
                        COALESCE(SUM(CASE WHEN latest_event.event_type = 'DROWSY' THEN 1 ELSE 0 END), 0)
                            AS drowsyWarningSessionCount,
                        COALESCE(SUM(CASE WHEN latest_event.event_type = 'SLEEP' THEN 1 ELSE 0 END), 0)
                            AS sleepWarningSessionCount
                    FROM monitoring_sessions ms
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    LEFT JOIN (
                        SELECT
                            ranked.session_id,
                            ranked.event_type
                        FROM (
                            SELECT
                                mel.session_id,
                                mel.event_type,
                                ROW_NUMBER() OVER (
                                    PARTITION BY mel.session_id
                                    ORDER BY mel.occurred_at_app DESC, mel.id DESC
                                ) AS rn
                            FROM monitoring_event_logs mel
                            WHERE mel.deleted_at IS NULL
                        ) ranked
                        WHERE ranked.rn = 1
                    ) latest_event
                      ON latest_event.session_id = ms.id
                    WHERE ms.ended_at_server IS NULL
                      AND ms.deleted_at IS NULL
                      AND ms.mode = 'ORGANIZATION'
                    """,
            nativeQuery = true
    )
    MonitoringSessionRealtimeSummaryProjection findRealtimeSummaryByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(
            value = """
                    SELECT
                        m.user_id AS userId,
                        u.email AS email,
                        u.name AS name,
                        u.nickname AS nickname,
                        COUNT(ms.id) AS totalSessionCount,
                        COALESCE(SUM(ms.drowsy_count), 0) AS drowsyCount,
                        COALESCE(SUM(ms.sleep_count), 0) AS sleepCount,
                        COALESCE(SUM(ms.drowsy_count + ms.sleep_count), 0) AS totalRiskCount
                    FROM member m
                    JOIN users u
                      ON u.id = m.user_id
                     AND u.deleted_at IS NULL
                    LEFT JOIN monitoring_sessions ms
                      ON ms.user_id = m.user_id
                     AND ms.deleted_at IS NULL
                     AND ms.mode = 'ORGANIZATION'
                    WHERE m.organization_id = :organizationId
                      AND m.deleted_at IS NULL
                    GROUP BY m.user_id, u.email, u.name, u.nickname
                    ORDER BY totalRiskCount DESC, sleepCount DESC, drowsyCount DESC, totalSessionCount DESC, m.user_id ASC
                    """,
            nativeQuery = true
    )
    List<OrganizationRiskUserProjection> findRiskUsersByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(
            value = """
                    SELECT
                        ms.id AS sessionId,
                        ms.user_id AS userId,
                        u.name AS userName,
                        ms.started_at_app AS startedAtApp,
                        ms.ended_at_app AS endedAtApp,
                        ms.duration_minutes AS durationMinutes,
                        COALESCE(ms.drowsy_count, 0) AS drowsyCount,
                        COALESCE(ms.sleep_count, 0) AS sleepCount,
                        COALESCE(ms.drowsy_count + ms.sleep_count, 0) AS totalRiskCount
                    FROM monitoring_sessions ms
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    JOIN users u
                      ON u.id = ms.user_id
                     AND u.deleted_at IS NULL
                    WHERE ms.deleted_at IS NULL
                      AND ms.mode = 'ORGANIZATION'
                      AND ms.ended_at_server IS NOT NULL
                    ORDER BY ms.ended_at_server DESC, ms.id DESC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<MonitoringRecentEndedSessionProjection> findRecentEndedSessionsByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT
                        YEAR(ms.started_at_server) AS year,
                        MONTH(ms.started_at_server) AS month,
                        DAY(ms.started_at_server) AS day,
                        HOUR(ms.started_at_server) AS hour,
                        COUNT(ms.id) AS sessionCount
                    FROM monitoring_sessions ms
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    WHERE ms.deleted_at IS NULL
                      AND ms.mode = 'ORGANIZATION'
                      AND ms.started_at_server >= :rangeStart
                      AND ms.started_at_server < :rangeEndExclusive
                    GROUP BY YEAR(ms.started_at_server), MONTH(ms.started_at_server), DAY(ms.started_at_server), HOUR(ms.started_at_server)
                    ORDER BY year ASC, month ASC, day ASC, hour ASC
                    """,
            nativeQuery = true
    )
    List<TimeBucketSessionCountProjection> findHourlySessionCountsByOrganizationAndRange(
            @Param("organizationId") Long organizationId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );

    @Query(
            value = """
                    SELECT
                        m.organization_id AS organizationId,
                        COUNT(ms.id) AS sessionCount
                    FROM monitoring_sessions ms
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.deleted_at IS NULL
                    WHERE ms.deleted_at IS NULL
                      AND ms.mode = 'ORGANIZATION'
                      AND ms.started_at_server >= :rangeStart
                      AND ms.started_at_server < :rangeEndExclusive
                    GROUP BY m.organization_id
                    """,
            nativeQuery = true
    )
    List<OrganizationSessionCountProjection> findOrganizationSessionCountsByRange(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );

    @Query(
            value = """
                    SELECT
                        :organizationId AS organizationId,
                        COUNT(ms.id) AS sessionCount
                    FROM monitoring_sessions ms
                    JOIN member m
                      ON m.user_id = ms.user_id
                     AND m.organization_id = :organizationId
                     AND m.deleted_at IS NULL
                    WHERE ms.deleted_at IS NULL
                      AND ms.mode = 'ORGANIZATION'
                      AND ms.started_at_server >= :rangeStart
                      AND ms.started_at_server < :rangeEndExclusive
                    """,
            nativeQuery = true
    )
    OrganizationSessionCountProjection findSingleOrganizationSessionCountByRange(
            @Param("organizationId") Long organizationId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive
    );
}
