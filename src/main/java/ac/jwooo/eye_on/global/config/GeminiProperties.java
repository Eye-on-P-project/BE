package ac.jwooo.eye_on.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String endpoint,
        int timeoutSeconds,
        int maxOutputTokens
) {

    private static final String DEFAULT_MODEL = "gemini-3.5-flash";
    private static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final int DEFAULT_TIMEOUT_SECONDS = 8;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 80;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String normalizedModel() {
        if (model == null || model.isBlank()) {
            return DEFAULT_MODEL;
        }
        return model.trim().replaceFirst("^models/", "");
    }

    public String normalizedEndpoint() {
        String value = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public int normalizedTimeoutSeconds() {
        return timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public int normalizedMaxOutputTokens() {
        return maxOutputTokens > 0 ? maxOutputTokens : DEFAULT_MAX_OUTPUT_TOKENS;
    }
}
