package ac.jwooo.eye_on.domain.organization.application.dto.request;

import java.util.Locale;

public enum RiskStatsGranularity {
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR;

    public static RiskStatsGranularity from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "HOUR", "HOURLY" -> HOUR;
            case "DAY", "DAILY" -> DAY;
            case "WEEK", "WEEKLY" -> WEEK;
            case "MONTH", "MONTHLY" -> MONTH;
            case "YEAR", "YEARLY" -> YEAR;
            default -> null;
        };
    }
}
