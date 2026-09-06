package br.com.vitrine7.print.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.printer-agent")
public record PrinterAgentProperties(
        String token,
        Duration longPollMax,
        Duration offlineAfter
) {
    public PrinterAgentProperties {
        longPollMax = positiveOr(longPollMax, Duration.ofSeconds(25));
        if (longPollMax.compareTo(Duration.ofSeconds(25)) > 0) {
            longPollMax = Duration.ofSeconds(25);
        }
        offlineAfter = positiveOr(offlineAfter, Duration.ofSeconds(90));
    }

    private static Duration positiveOr(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative()
                ? fallback
                : value;
    }
}
