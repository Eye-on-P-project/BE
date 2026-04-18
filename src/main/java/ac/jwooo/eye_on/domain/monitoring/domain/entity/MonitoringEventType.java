package ac.jwooo.eye_on.domain.monitoring.domain.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MonitoringEventType {
    DROWSY,
    SLEEP,
    NORMAL;

    @JsonCreator
    public static MonitoringEventType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "DROWSY" -> DROWSY;
            case "SLEEP", "SLEEPING" -> SLEEP;
            case "NORMAL" -> NORMAL;
            default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입입니다: " + value);
        };
    }
}
