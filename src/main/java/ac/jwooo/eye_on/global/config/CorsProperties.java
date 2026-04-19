package ac.jwooo.eye_on.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(String allowedOrigins) {

    public List<String> allowedOriginList() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of("http://localhost:5173");
        }
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins does not support '*' when credentials are enabled. Use explicit origins."
            );
        }

        return origins;
    }
}
