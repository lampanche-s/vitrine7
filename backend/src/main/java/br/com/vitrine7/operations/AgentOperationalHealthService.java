package br.com.vitrine7.operations;

import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeProperties;
import br.com.vitrine7.payment.terminal.bridge.TerminalDeviceRepository;
import br.com.vitrine7.print.repository.PrinterAgentHealthRepository;
import br.com.vitrine7.print.security.PrinterAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentOperationalHealthService {
    private final PrinterAgentHealthRepository printerRepository;
    private final TerminalDeviceRepository terminalRepository;
    private final PrinterAgentProperties printerProperties;
    private final TerminalBridgeProperties terminalProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AgentOperationalHealthResponse status() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return new AgentOperationalHealthResponse(
                now,
                List.of(printerStatus(now), pagBankStatus(now))
        );
    }

    private AgentOperationalHealthResponse.Agent printerStatus(
            OffsetDateTime now
    ) {
        PrinterAgentHealthRepository.Snapshot snapshot =
                printerRepository.latest();
        AgentOperationalState state;
        if (printerProperties.token() == null
                || printerProperties.token().isBlank()) {
            state = AgentOperationalState.NOT_CONFIGURED;
        } else if (snapshot != null && snapshot.revokedAt() != null) {
            state = AgentOperationalState.REVOKED;
        } else if (snapshot != null
                && !snapshot.lastSeenAt().isBefore(
                now.minus(printerProperties.offlineAfter()))) {
            state = AgentOperationalState.ONLINE;
        } else {
            state = AgentOperationalState.OFFLINE;
        }
        return new AgentOperationalHealthResponse.Agent(
                "PRINTER",
                state,
                snapshot == null ? null : snapshot.identity(),
                snapshot == null ? null : snapshot.agentVersion(),
                snapshot == null ? null : snapshot.lastSeenAt()
        );
    }

    private AgentOperationalHealthResponse.Agent pagBankStatus(
            OffsetDateTime now
    ) {
        TerminalDeviceRepository.OperationalSnapshot snapshot =
                terminalRepository.pagBankOperationalSnapshot();
        AgentOperationalState state;
        if (snapshot == null || snapshot.total() == 0) {
            state = AgentOperationalState.NOT_CONFIGURED;
        } else if (snapshot.active() == 0
                && snapshot.revoked() == snapshot.total()) {
            state = AgentOperationalState.REVOKED;
        } else if (snapshot.active() > 0
                && snapshot.lastSeenAt() != null
                && !snapshot.lastSeenAt().isBefore(
                now.minus(terminalProperties.offlineAfter()))) {
            state = AgentOperationalState.ONLINE;
        } else {
            state = AgentOperationalState.OFFLINE;
        }
        return new AgentOperationalHealthResponse.Agent(
                "PAGBANK",
                state,
                null,
                snapshot == null ? null : snapshot.agentVersion(),
                snapshot == null ? null : snapshot.lastSeenAt()
        );
    }
}
