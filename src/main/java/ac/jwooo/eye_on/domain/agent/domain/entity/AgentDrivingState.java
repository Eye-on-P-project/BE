package ac.jwooo.eye_on.domain.agent.domain.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AgentDrivingState {
    NORMAL,
    AWAKE,
    DROWSY,
    SLEEP;

    @JsonCreator
    public static AgentDrivingState from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "NORMAL" -> NORMAL;
            case "AWAKE" -> AWAKE;
            case "DROWSY" -> DROWSY;
            case "SLEEP", "SLEEPING" -> SLEEP;
            default -> throw new IllegalArgumentException("지원하지 않는 운전자 상태입니다: " + value);
        };
    }
}
