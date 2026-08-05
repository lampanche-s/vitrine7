package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestNormalizer {

    public String normalizeManualReason(
            String reason
    ) {
        if (reason == null) {
            throw invalidReason();
        }

        String normalized = reason
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.length() < 3
                || normalized.length() > 255) {

            throw invalidReason();
        }

        return normalized;
    }

    public void validateManualMethod(
            PaymentMethod method
    ) {
        if (method == null
                || !method.supportsManualFallback()) {

            throw new BusinessException(
                    "INVALID_MANUAL_PAYMENT_METHOD",
                    "A confirmacao manual aceita apenas credito, debito ou Pix."
            );
        }
    }

    private BusinessException invalidReason() {
        return new BusinessException(
                "INVALID_MANUAL_PAYMENT_REASON",
                "O motivo da confirmacao manual deve possuir entre 3 e 255 caracteres."
        );
    }
}
