package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.TerminalSimulationOutcome;

import java.util.UUID;

public record TerminalAdapterCommand(
        UUID checkoutId,
        UUID paymentId,
        PaymentTerminalProvider provider,
        PaymentMethod method,
        long amountCents,
        TerminalSimulationOutcome simulatedOutcome
) {
}
