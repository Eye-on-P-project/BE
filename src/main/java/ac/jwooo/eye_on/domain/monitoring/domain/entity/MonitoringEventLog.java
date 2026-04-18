package ac.jwooo.eye_on.domain.monitoring.domain.entity;

import java.math.BigDecimal;
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
@Table(name = "monitoring_event_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoringEventLog extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private MonitoringEventType eventType;

    @Column(name = "occurred_at_app", nullable = false)
    private LocalDateTime occurredAtApp;

    @Column(name = "occurred_at_server", nullable = false)
    private LocalDateTime occurredAtServer;

    @Column(name = "resolved_at_app")
    private LocalDateTime resolvedAtApp;

    @Column(name = "resolved_at_server")
    private LocalDateTime resolvedAtServer;

    @Column(name = "duration_seconds", precision = 10, scale = 2)
    private BigDecimal durationSeconds;

    @Builder(access = AccessLevel.PRIVATE)
    private MonitoringEventLog(
            Long sessionId,
            MonitoringEventType eventType,
            LocalDateTime occurredAtApp,
            LocalDateTime occurredAtServer,
            LocalDateTime resolvedAtApp,
            LocalDateTime resolvedAtServer,
            BigDecimal durationSeconds
    ) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.occurredAtApp = occurredAtApp;
        this.occurredAtServer = occurredAtServer;
        this.resolvedAtApp = resolvedAtApp;
        this.resolvedAtServer = resolvedAtServer;
        this.durationSeconds = durationSeconds;
    }

    public static MonitoringEventLog create(
            Long sessionId,
            MonitoringEventType eventType,
            LocalDateTime occurredAtApp,
            LocalDateTime occurredAtServer
    ) {
        return MonitoringEventLog.builder()
                .sessionId(sessionId)
                .eventType(eventType)
                .occurredAtApp(occurredAtApp)
                .occurredAtServer(occurredAtServer)
                .build();
    }

    public boolean isResolved() {
        return resolvedAtServer != null;
    }

    public void resolve(
            LocalDateTime resolvedAtApp,
            LocalDateTime resolvedAtServer,
            BigDecimal durationSeconds
    ) {
        this.resolvedAtApp = resolvedAtApp;
        this.resolvedAtServer = resolvedAtServer;
        this.durationSeconds = durationSeconds;
    }
}
