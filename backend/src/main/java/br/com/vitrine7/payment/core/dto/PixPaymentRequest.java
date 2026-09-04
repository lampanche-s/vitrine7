package br.com.vitrine7.payment.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PixPaymentRequest(
        @Min(value = 1, message = "O valor deve ser maior que zero.")
        @Max(value = 999_999_999, message = "O valor excede o limite permitido.")
        Long amountCents
) {
}
