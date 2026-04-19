package ac.jwooo.eye_on.domain.monitoring.domain.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringNotificationResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MonitoringRealtimeSseBroker {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long SSE_TIMEOUT_MS = 0L;

    private final Map<Long, Set<SseEmitter>> emittersByOrganizationId = new ConcurrentHashMap<>();

    public SseEmitter connect(Long organizationId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emittersByOrganizationId.computeIfAbsent(organizationId, id -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(organizationId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(organizationId, emitter);
            emitter.complete();
        });
        emitter.onError(exception -> {
            removeEmitter(organizationId, emitter);
            emitter.completeWithError(exception);
        });

        send(organizationId, "connected", Map.of(
                "organizationId", organizationId,
                "connectedAt", LocalDateTime.now(KST).withNano(0)
        ));
        return emitter;
    }

    public void sendSummary(Long organizationId, MonitoringRealtimeSummaryResponse summary) {
        send(organizationId, "summary", summary);
    }

    public void sendAlert(Long organizationId, MonitoringNotificationResponse notificationResponse) {
        send(organizationId, "alert", notificationResponse);
    }

    @Scheduled(fixedDelay = 15000)
    public void heartbeat() {
        LocalDateTime now = LocalDateTime.now(KST).withNano(0);
        for (Long organizationId : emittersByOrganizationId.keySet()) {
            send(organizationId, "heartbeat", Map.of("at", now));
        }
    }

    private void send(Long organizationId, String eventName, Object data) {
        Set<SseEmitter> emitters = emittersByOrganizationId.get(organizationId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException exception) {
                log.debug("SSE send failed. orgId={}, event={}", organizationId, eventName, exception);
                removeEmitter(organizationId, emitter);
                emitter.complete();
            }
        }
    }

    private void removeEmitter(Long organizationId, SseEmitter emitter) {
        emittersByOrganizationId.computeIfPresent(organizationId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
