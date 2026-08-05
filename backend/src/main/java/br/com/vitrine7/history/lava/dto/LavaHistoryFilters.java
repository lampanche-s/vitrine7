package br.com.vitrine7.history.lava.dto;

import br.com.vitrine7.history.bar.dto.HistoryPaymentMethod;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;

import java.time.OffsetDateTime;

public record LavaHistoryFilters(
        int page,
        int size,
        String search,
        String normalizedSearch,
        LavaWorkOrderStatus status,
        OffsetDateTime from,
        OffsetDateTime to,
        HistoryPaymentMethod paymentMethod
) {
}
