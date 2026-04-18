package ac.jwooo.eye_on.domain.monitoring.domain.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MonitoringMode {
    DRIVING,
    STUDY;

    @JsonCreator
    public static MonitoringMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return MonitoringMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

