package br.com.vitrine7.checkout.entity;

public enum CheckoutStatus {

    DRAFT,
    READY_FOR_PAYMENT,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    PAID,
    FINALIZED,
    CANCELLED,
    EXPIRED;

    public boolean canExpire() {
        return this == DRAFT
                || this == READY_FOR_PAYMENT
                || this == PAYMENT_FAILED;
    }

    public boolean canCancelBeforePayment() {
        return this == DRAFT
                || this == READY_FOR_PAYMENT
                || this == PAYMENT_FAILED;
    }

    public boolean isTerminal() {
        return this == FINALIZED
                || this == CANCELLED
                || this == EXPIRED;
    }
}
