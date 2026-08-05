package br.com.vitrine7.bar.tab.dto;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;

import java.time.OffsetDateTime;

public record BarTabCancellationResponse(
        Long id,
        String name,
        String status,
        OffsetDateTime cancelledAt,
        Long cancelledByUserId,
        String cancellationReason
) {

    public static BarTabCancellationResponse from(
            BarTabEntity tab
    ) {
        return new BarTabCancellationResponse(
                tab.getId(),
                tab.getName(),
                tab.getStatus().name(),
                tab.getCancelledAt(),
                tab.getCancelledByUserId(),
                tab.getCancellationReason()
        );
    }
}
