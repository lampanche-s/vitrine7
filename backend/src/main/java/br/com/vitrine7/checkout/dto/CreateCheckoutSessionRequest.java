package br.com.vitrine7.checkout.dto;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(

        @NotNull(
                message = "Informe o tipo da operacao."
        )
        CheckoutOperationType operationType
) {
}
