package br.com.vitrine7.payment.core.dto;

import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;

public record PaymentConfirmationResponse(
        PaymentResponse payment,
        CheckoutSessionResponse checkout
) {
}
