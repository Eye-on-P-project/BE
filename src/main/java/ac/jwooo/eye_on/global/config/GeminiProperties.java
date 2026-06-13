package ac.jwooo.eye_on.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String provider,
        String apiKey,
        String model,
        String endpoint,
        String vertexEndpoint,
        String projectId,
        String location,
        int timeoutSeconds,
        int maxOutputTokens,
        int thinkingBudget
) {

    private static final String PROVIDER_AI_STUDIO = "ai-studio";
    private static final String PROVIDER_VERTEX = "vertex";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String DEFAULT_LOCATION = "global";
    private static final String GLOBAL_VERTEX_ENDPOINT = "https://aiplatform.googleapis.com/v1";
    private static final int DEFAULT_TIMEOUT_SECONDS = 8;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 512;
    private static final int DEFAULT_THINKING_BUDGET = 0;

    public boolean isVertexProvider() {
        return PROVIDER_VERTEX.equals(normalizedProvider());
    }

    public String normalizedProvider() {
        if (provider == null || provider.isBlank()) {
            return PROVIDER_VERTEX;
        }
        return provider.trim().toLowerCase();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean hasConfiguredProjectId() {
        return projectId != null && !projectId.isBlank();
    }

    public String normalizedModel() {
        if (model == null || model.isBlank()) {
            return DEFAULT_MODEL;
        }
        return model.trim()
                .replaceFirst("^publishers/google/models/", "")
                .replaceFirst("^models/", "");
    }

    public String normalizedEndpoint() {
        String value = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public String normalizedVertexEndpoint() {
        if (vertexEndpoint != null && !vertexEndpoint.isBlank()) {
            return trimTrailingSlash(vertexEndpoint.trim());
        }
        String locationValue = normalizedLocation();
        if ("global".equals(locationValue)) {
            return GLOBAL_VERTEX_ENDPOINT;
        }
        return "https://%s-aiplatform.googleapis.com/v1".formatted(locationValue);
    }

    public String normalizedProjectId() {
        return projectId == null ? "" : projectId.trim();
    }

    public String normalizedLocation() {
        if (location == null || location.isBlank()) {
            return DEFAULT_LOCATION;
        }
        return location.trim();
    }

    public int normalizedTimeoutSeconds() {
        return timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public int normalizedMaxOutputTokens() {
        if (maxOutputTokens <= 0) {
            return DEFAULT_MAX_OUTPUT_TOKENS;
        }
        return Math.max(maxOutputTokens, DEFAULT_MAX_OUTPUT_TOKENS);
    }

    public int normalizedThinkingBudget() {
        return Math.max(thinkingBudget, DEFAULT_THINKING_BUDGET);
    }

    public String replySource() {
        return isVertexProvider() ? "GEMINI_VERTEX" : "GEMINI";
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
