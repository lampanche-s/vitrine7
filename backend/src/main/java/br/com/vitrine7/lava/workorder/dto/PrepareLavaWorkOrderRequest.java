package br.com.vitrine7.lava.workorder.dto;

import br.com.vitrine7.checkout.entity.CheckoutDocumentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrepareLavaWorkOrderRequest(
        @NotNull(message = "Informe o tipo de comprovante.")
        CheckoutDocumentType documentType,

        @Size(max = 20)
        String cpf,

        @Min(0)
        Long discountCents
) {
}
