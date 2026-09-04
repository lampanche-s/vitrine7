package br.com.vitrine7.bar.tab.dto;

import br.com.vitrine7.checkout.entity.CheckoutDocumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrepareBarTabRequest(

        @NotNull(message = "Informe o tipo de comprovante.")
        CheckoutDocumentType documentType,

        @Size(max = 20)
        String cpf,

        @NotNull(message = "Informe o desconto.")
        @Min(value = 0)
        @Max(value = 99_999_999)
        Long discountCents,

        @Size(max = 120)
        String vehicleName,

        @Size(max = 20)
        String vehiclePlate
) {
}
