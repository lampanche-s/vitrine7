package br.com.vitrine7.payment.terminal.bridge;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.payment-terminal.bridge")
public record TerminalBridgeProperties(
        Duration commandTimeout,
        Duration longPollMax,
        Duration offlineAfter,
        Duration pairingCodeTtl
) {
    public TerminalBridgeProperties {
        commandTimeout = valueOr(commandTimeout, Duration.ofSeconds(120));
        longPollMax = cap(valueOr(longPollMax, Duration.ofSeconds(25)), Duration.ofSeconds(25));
        offlineAfter = valueOr(offlineAfter, Duration.ofSeconds(90));
        pairingCodeTtl = valueOr(pairingCodeTtl, Duration.ofMinutes(10));
    }

    private static Duration valueOr(Duration value, Duration fallback) {
        return value == null || value.isNegative() || value.isZero() ? fallback : value;
    }

    private static Duration cap(Duration value, Duration maximum) {
        return value.compareTo(maximum) > 0 ? maximum : value;
    }
}
