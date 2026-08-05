package br.com.vitrine7.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtIssuer,
        String jwtSecret,
        Duration jwtExpiration,
        Duration inactivityExpiration,
        Duration activityTouchInterval,
        String authCookieName,
        boolean cookieSecure,
        String cookieSameSite,
        String allowedOrigins,
        InitialAdmin initialAdmin,
        SuperAdmin superAdmin
) {

    public List<String> getAllowedOriginList() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of();
        }

        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public record InitialAdmin(
            String name,
            String username,
            String password
    ) {
    }

    public record SuperAdmin(
            String username,
            String password
    ) {
    }
}
