package br.com.vitrine7.finance.dto;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;

import java.time.Instant;
import java.time.LocalDate;

public record FinanceFilters(
        LocalDate from,
        LocalDate to,
        Instant fromInstant,
        Instant toExclusiveInstant,
        FinanceModule module,
        CheckoutOperationType operationType,
        FinancePaymentMethod paymentMethod,
        String normalizedSearch,
        String timeZone
) {
}
