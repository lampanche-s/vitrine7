package br.com.vitrine7.payment.terminal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TerminalPaymentReversalRequest(

        @NotBlank(
                message = "O motivo do estorno e obrigatorio."
        )
        @Size(
                min = 3,
                max = 255,
                message = "O motivo deve possuir entre 3 e 255 caracteres."
        )
        String reason,

        @AssertTrue(
                message = "Confirme que o cartao e o portador estao presentes."
        )
        boolean cardholderPresent
) {
}
