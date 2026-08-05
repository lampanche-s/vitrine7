package br.com.vitrine7.pagbankagent;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

public record AgentConfig(
        URI bridgeBaseUrl,
        Path tokenStorePath,
        String pairingCode,
        String platform,
        String agentVersion,
        String externalTerminalReference,
        PaymentTerminalDriverMode driverMode,
        PaymentOutcomeMode developmentOutcome,
        Path plugPagJarPath,
        Path plugPagNativePath,
        String plugPagBluetoothAddress,
        String plugPagAppName,
        String plugPagAppVersion,
        Duration heartbeatInterval,
        int longPollSeconds
) {
    public static AgentConfig fromEnvironment() {
        String baseUrl = value("V7_PAGBANK_AGENT_BRIDGE_URL", "http://localhost:8080");
        String tokenPath = value(
                "V7_PAGBANK_AGENT_TOKEN_STORE",
                Path.of(System.getProperty("user.home"), ".vitrine7", "pagbank-agent", "device-token.properties").toString()
        );
        return new AgentConfig(
                URI.create(baseUrl),
                Path.of(tokenPath),
                value("V7_PAGBANK_AGENT_PAIRING_CODE", null),
                value("V7_PAGBANK_AGENT_PLATFORM", "LOCAL"),
                value("V7_PAGBANK_AGENT_VERSION", "pagbank-agent-dev"),
                value("V7_PAGBANK_AGENT_TERMINAL_REFERENCE", "PAGBANK-LOCAL-DEV"),
                PaymentTerminalDriverMode.valueOf(
                        value("V7_PAGBANK_AGENT_DRIVER", "SIMULATED")
                                .trim()
                                .toUpperCase(Locale.ROOT)
                ),
                PaymentOutcomeMode.valueOf(
                        value("V7_PAGBANK_AGENT_OUTCOME", "APPROVED")
                                .trim()
                                .toUpperCase(Locale.ROOT)
                ),
                pathValue("V7_PAGBANK_AGENT_PLUGPAG_JAR"),
                pathValue("V7_PAGBANK_AGENT_PLUGPAG_NATIVE_DIR"),
                value("V7_PAGBANK_AGENT_PLUGPAG_BT_ADDRESS", null),
                value("V7_PAGBANK_AGENT_PLUGPAG_APP_NAME", "Vitrine 7"),
                value("V7_PAGBANK_AGENT_PLUGPAG_APP_VERSION", "pagbank-agent-dev"),
                Duration.ofSeconds(longValue("V7_PAGBANK_AGENT_HEARTBEAT_SECONDS", 20)),
                (int) longValue("V7_PAGBANK_AGENT_LONG_POLL_SECONDS", 10)
        );
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static long longValue(String name, long fallback) {
        String value = value(name, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Path pathValue(String name) {
        String value = value(name, null);
        return value == null ? null : Path.of(value);
    }

    public URI bridgeUri(String pathAndQuery) {
        String base = bridgeBaseUrl.toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + "/api/v1/payment-terminal/bridge" + pathAndQuery);
    }
}
