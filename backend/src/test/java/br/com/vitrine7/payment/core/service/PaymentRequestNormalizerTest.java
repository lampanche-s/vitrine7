package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentRequestNormalizerTest {

    private final PaymentRequestNormalizer normalizer =
            new PaymentRequestNormalizer();

    @Test
    void manualFallbackAcceptsCreditDebitAndPixForNewPayments() {
        assertDoesNotThrow(() ->
                normalizer.validateManualMethod(PaymentMethod.CREDIT_CARD)
        );
        assertDoesNotThrow(() ->
                normalizer.validateManualMethod(PaymentMethod.DEBIT_CARD)
        );
        assertDoesNotThrow(() ->
                normalizer.validateManualMethod(PaymentMethod.PIX)
        );

        BusinessException cashException = assertThrows(
                BusinessException.class,
                () -> normalizer.validateManualMethod(PaymentMethod.CASH)
        );

        assertEquals("INVALID_MANUAL_PAYMENT_METHOD", cashException.getCode());
    }
}
