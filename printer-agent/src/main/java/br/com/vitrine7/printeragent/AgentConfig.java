package br.com.vitrine7.printeragent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

public record AgentConfig(
        String backendUrl,
        String agentToken,
        String printerName,
        int waitSeconds,
        double paperWidthMm,
        double marginMm,
        float fontSizePt,
        float lineHeightPt
) {
    public static AgentConfig load(Path configFile, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();

        if (Files.exists(configFile)) {
            try (InputStream input = Files.newInputStream(configFile)) {
                properties.load(input);
            }
        }

        String backendUrl = value(environment, "V7_PRINTER_BACKEND_URL", properties, "backend.url", "");
        String token = value(environment, "V7_PRINTER_AGENT_TOKEN", properties, "agent.token", "");
        String printerName = value(environment, "V7_PRINTER_NAME", properties, "printer.name", "");
        int waitSeconds = integer(value(environment, "V7_PRINTER_WAIT_SECONDS", properties, "poll.wait.seconds", "20"), 20, 0, 25);
        double width = decimal(value(environment, "V7_PRINTER_PAPER_WIDTH_MM", properties, "paper.width.mm", "80"), 80d, 40d, 120d);
        double margin = decimal(value(environment, "V7_PRINTER_MARGIN_MM", properties, "paper.margin.mm", "2"), 2d, 0d, 10d);
        float fontSize = (float) decimal(value(environment, "V7_PRINTER_FONT_SIZE_PT", properties, "font.size.pt", "7"), 7d, 5d, 12d);
        float lineHeight = (float) decimal(value(environment, "V7_PRINTER_LINE_HEIGHT_PT", properties, "line.height.pt", "9"), 9d, 6d, 18d);

        if (backendUrl.isBlank()) {
            throw new IllegalStateException("backend.url nao foi configurado.");
        }
        if (token.isBlank()) {
            throw new IllegalStateException("agent.token nao foi configurado.");
        }
        if (printerName.isBlank()) {
            throw new IllegalStateException("printer.name nao foi configurado.");
        }

        return new AgentConfig(
                stripTrailingSlash(backendUrl.trim()),
                token.trim(),
                printerName.trim(),
                waitSeconds,
                width,
                margin,
                fontSize,
                lineHeight
        );
    }

    public Duration requestTimeout() {
        return Duration.ofSeconds(Math.max(10, waitSeconds + 10L));
    }

    private static String value(
            Map<String, String> environment,
            String environmentName,
            Properties properties,
            String propertyName,
            String fallback
    ) {
        String environmentValue = environment.get(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return properties.getProperty(propertyName, fallback);
    }

    private static int integer(String value, int fallback, int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double decimal(String value, double fallback, double minimum, double maximum) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String stripTrailingSlash(String value) {
        String current = value;
        while (current.endsWith("/")) {
            current = current.substring(0, current.length() - 1);
        }
        return current;
    }
}
