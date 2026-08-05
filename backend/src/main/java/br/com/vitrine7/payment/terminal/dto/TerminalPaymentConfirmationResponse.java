package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;
import br.com.vitrine7.payment.core.dto.PaymentResponse;

public record TerminalPaymentConfirmationResponse(
        PaymentResponse payment,
        PaymentTerminalTransactionResponse terminalTransaction,
        CheckoutSessionResponse checkout
) {
}
