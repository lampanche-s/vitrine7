package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.core.dto.PaymentResponse;

import java.util.UUID;

public record TerminalPaymentReversalResponse(
        UUID commandId,
        PaymentResponse payment,
        PaymentTerminalTransactionResponse
                terminalTransaction
) {
}
