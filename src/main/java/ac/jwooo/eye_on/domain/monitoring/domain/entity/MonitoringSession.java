package ac.jwooo.eye_on.domain.monitoring.domain.entity;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "monitoring_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoringSession extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MonitoringMode mode;

    @Column(name = "started_at_app", nullable = false)
    private LocalDateTime startedAtApp;

    @Column(name = "started_at_server", nullable = false)
    private LocalDateTime startedAtServer;

    @Column(name = "ended_at_app")
    private LocalDateTime endedAtApp;

    @Column(name = "ended_at_server")
    private LocalDateTime endedAtServer;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "drowsy_count", nullable = false)
    private Integer drowsyCount;

    @Column(name = "sleep_count", nullable = false)
    private Integer sleepCount;

    @Builder(access = AccessLevel.PRIVATE)
    private MonitoringSession(
            Long userId,
            MonitoringMode mode,
            LocalDateTime startedAtApp,
            LocalDateTime startedAtServer,
            LocalDateTime endedAtApp,
            LocalDateTime endedAtServer,
            Integer durationMinutes,
            Integer drowsyCount,
            Integer sleepCount
    ) {
        this.userId = userId;
        this.mode = mode;
        this.startedAtApp = startedAtApp;
        this.startedAtServer = startedAtServer;
        this.endedAtApp = endedAtApp;
        this.endedAtServer = endedAtServer;
        this.durationMinutes = durationMinutes;
        this.drowsyCount = drowsyCount;
        this.sleepCount = sleepCount;
    }

    public static MonitoringSession create(
            Long userId,
            MonitoringMode mode,
            LocalDateTime startedAtApp,
            LocalDateTime startedAtServer
    ) {
        return MonitoringSession.builder()
                .userId(userId)
                .mode(mode)
                .startedAtApp(startedAtApp)
                .startedAtServer(startedAtServer)
                .durationMinutes(0)
                .drowsyCount(0)
                .sleepCount(0)
                .build();
    }

    public boolean isEnded() {
        return endedAtServer != null;
    }

    public void end(LocalDateTime endedAtApp, LocalDateTime endedAtServer, int durationMinutes) {
        this.endedAtApp = endedAtApp;
        this.endedAtServer = endedAtServer;
        this.durationMinutes = durationMinutes;
    }

    public void increaseEventCount(MonitoringEventType eventType) {
        if (eventType == MonitoringEventType.DROWSY) {
            drowsyCount += 1;
            return;
        }
        sleepCount += 1;
    }
}

