package br.com.vitrine7.print.service;

import br.com.vitrine7.print.repository.PrinterAgentHealthRepository;
import br.com.vitrine7.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrinterAgentHealthServiceTest {
    private final PrinterAgentHealthRepository repository =
            mock(PrinterAgentHealthRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-04T12:00:00Z"),
            ZoneOffset.UTC
    );
    private final PrinterAgentHealthService service =
            new PrinterAgentHealthService(repository, clock);

    @Test
    void defaultsMissingMetadataForLegacyAgents() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        when(repository.heartbeat(
                PrinterAgentHealthService.LEGACY_IDENTITY,
                PrinterAgentHealthService.LEGACY_VERSION,
                now
        )).thenReturn(true);

        var response = service.heartbeat(null, null);

        assertEquals("ONLINE", response.status());
        verify(repository).heartbeat(
                PrinterAgentHealthService.LEGACY_IDENTITY,
                PrinterAgentHealthService.LEGACY_VERSION,
                now
        );
    }

    @Test
    void doesNotReviveRevokedIdentity() {
        when(repository.heartbeat(
                "caixa-principal",
                "1.0.0",
                OffsetDateTime.now(clock)
        )).thenReturn(false);

        assertEquals(
                "REVOKED",
                service.heartbeat("caixa-principal", "1.0.0").status()
        );
    }

    @Test
    void rejectsOversizedIdentityInsteadOfTruncatingIt() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.heartbeat("x".repeat(121), "1.0.0")
        );
    }
}
