package ac.jwooo.eye_on.domain.monitoring.domain.entity;

import java.time.LocalDateTime;

import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "monitoring_event_logs",
        indexes = {
                @Index(name = "idx_mel_session_latest", columnList = "session_id,deleted_at,occurred_at_app,id,event_type"),
                @Index(name = "idx_mel_range_event", columnList = "occurred_at_app,event_type,deleted_at,session_id")
        }
)
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

    @Column(name = "occurred_at_app", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime occurredAtApp;

    @Column(name = "occurred_at_server", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime occurredAtServer;

    @Builder(access = AccessLevel.PRIVATE)
    private MonitoringEventLog(
            Long sessionId,
            MonitoringEventType eventType,
            LocalDateTime occurredAtApp,
            LocalDateTime occurredAtServer
    ) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.occurredAtApp = occurredAtApp;
        this.occurredAtServer = occurredAtServer;
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
}
