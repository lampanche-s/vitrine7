package br.com.vitrine7.payment.core.entity;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    PIX,
    CASH;

    public boolean supportsManualFallback() {
        return this == CREDIT_CARD
                || this == DEBIT_CARD
                || this == PIX;
    }
}
