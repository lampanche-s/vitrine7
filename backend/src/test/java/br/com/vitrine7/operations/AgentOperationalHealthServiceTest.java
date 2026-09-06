package br.com.vitrine7.operations;

import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeProperties;
import br.com.vitrine7.payment.terminal.bridge.TerminalDeviceRepository;
import br.com.vitrine7.print.repository.PrinterAgentHealthRepository;
import br.com.vitrine7.print.security.PrinterAgentProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentOperationalHealthServiceTest {
    private final PrinterAgentHealthRepository printer =
            mock(PrinterAgentHealthRepository.class);
    private final TerminalDeviceRepository terminal =
            mock(TerminalDeviceRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-04T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void reportsFreshPrinterAndPagBankAsOnline() {
        OffsetDateTime seenAt = OffsetDateTime.now(clock).minusSeconds(30);
        when(printer.latest()).thenReturn(
                new PrinterAgentHealthRepository.Snapshot(
                        "caixa-principal", "1.0.0", seenAt, null
                )
        );
        when(terminal.pagBankOperationalSnapshot()).thenReturn(
                new TerminalDeviceRepository.OperationalSnapshot(
                        1, 1, 0, seenAt, "0.0.1"
                )
        );

        var response = service("configured-token").status();

        assertEquals(AgentOperationalState.ONLINE,
                response.agents().get(0).status());
        assertEquals(AgentOperationalState.ONLINE,
                response.agents().get(1).status());
    }

    @Test
    void distinguishesNotConfiguredOfflineAndRevoked() {
        OffsetDateTime old = OffsetDateTime.now(clock).minusMinutes(10);
        when(printer.latest()).thenReturn(
                new PrinterAgentHealthRepository.Snapshot(
                        "caixa-principal", "1.0.0", old, null
                )
        );
        when(terminal.pagBankOperationalSnapshot()).thenReturn(
                new TerminalDeviceRepository.OperationalSnapshot(
                        1, 0, 1, old, null
                )
        );

        var configured = service("configured-token").status();
        assertEquals(AgentOperationalState.OFFLINE,
                configured.agents().get(0).status());
        assertEquals(AgentOperationalState.REVOKED,
                configured.agents().get(1).status());

        var notConfigured = service("").status();
        assertEquals(AgentOperationalState.NOT_CONFIGURED,
                notConfigured.agents().get(0).status());
    }

    private AgentOperationalHealthService service(String printerToken) {
        return new AgentOperationalHealthService(
                printer,
                terminal,
                new PrinterAgentProperties(
                        printerToken,
                        Duration.ofSeconds(25),
                        Duration.ofSeconds(90)
                ),
                new TerminalBridgeProperties(
                        Duration.ofSeconds(120),
                        Duration.ofSeconds(25),
                        Duration.ofSeconds(90),
                        Duration.ofMinutes(10)
                ),
                clock
        );
    }
}
