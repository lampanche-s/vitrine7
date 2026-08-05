package br.com.vitrine7.history.bar.dto;

import java.time.OffsetDateTime;

public record BarHistoryFilters(
        int page,
        int size,
        String search,
        String normalizedSearch,
        OffsetDateTime from,
        OffsetDateTime to,
        HistoryPaymentMethod paymentMethod
) {
}
