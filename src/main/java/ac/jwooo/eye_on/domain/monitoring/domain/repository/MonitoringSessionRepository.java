package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.util.Optional;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {

    Optional<MonitoringSession> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUserIdAndEndedAtServerIsNullAndDeletedAtIsNull(Long userId);

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
                    """,
            nativeQuery = true
    )
    MonitoringSessionRealtimeSummaryProjection findRealtimeSummaryByOrganizationId(@Param("organizationId") Long organizationId);
}
