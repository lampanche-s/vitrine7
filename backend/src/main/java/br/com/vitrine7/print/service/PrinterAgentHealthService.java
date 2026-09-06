package br.com.vitrine7.print.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.print.repository.PrinterAgentHealthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PrinterAgentHealthService {
    public static final String LEGACY_IDENTITY = "vitrine7-printer-agent";
    public static final String LEGACY_VERSION = "legacy";

    private final PrinterAgentHealthRepository repository;
    private final Clock clock;

    @Transactional
    public Heartbeat heartbeat(String identity, String agentVersion) {
        String normalizedIdentity = normalize(
                identity,
                LEGACY_IDENTITY,
                120
        );
        String normalizedVersion = normalize(
                agentVersion,
                LEGACY_VERSION,
                60
        );
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean accepted = repository.heartbeat(
                normalizedIdentity,
                normalizedVersion,
                now
        );
        return new Heartbeat(
                normalizedIdentity,
                normalizedVersion,
                accepted ? "ONLINE" : "REVOKED",
                now
        );
    }

    private String normalize(String value, String fallback, int maximum) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.length() > maximum) {
            throw new InvalidRequestException(
                    "PRINTER_AGENT_METADATA_INVALID",
                    "Identidade ou versao do agente excede o limite."
            );
        }
        return normalized;
    }

    public record Heartbeat(
            String identity,
            String agentVersion,
            String status,
            OffsetDateTime lastSeenAt
    ) {
    }
}
