package ac.example.eye.on.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String refreshCookieName,
        boolean cookieSecure,
        String cookieSameSite,
        String cookiePath,
        String cookieDomain
) {
}

