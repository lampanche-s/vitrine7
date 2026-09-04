package br.com.vitrine7.payment.core.entity;

import java.util.EnumSet;
import java.util.Set;

public enum PaymentStatus {

    PENDING,
    PROCESSING,
    APPROVED,
    DECLINED,
    CANCELLED,
    REVERSED,
    SUPERSEDED;

    public static Set<PaymentStatus> settledStatuses() {
        return EnumSet.of(
                APPROVED,
                REVERSED
        );
    }
}
