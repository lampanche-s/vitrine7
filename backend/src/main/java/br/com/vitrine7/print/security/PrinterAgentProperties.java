package br.com.vitrine7.print.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.printer-agent")
public record PrinterAgentProperties(
        String token,
        Duration longPollMax
) {
}
