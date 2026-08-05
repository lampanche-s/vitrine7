package br.com.vitrine7.bar.tab.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BarTabResponse(
        Long id,
        String name,
        String status,
        UUID checkoutId,
        String checkoutStatus,
        Long subtotalCents,
        Long discountCents,
        Long totalCents,
        String documentType,
        String cpfDigits,
        boolean prepared,
        OffsetDateTime preparedAt,
        List<BarTabLineResponse> lines,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
