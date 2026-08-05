package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;

import java.time.OffsetDateTime;
import java.util.Map;

public record ProviderPaymentResult(
        ProviderPaymentStatus status,
        String providerReference,
        String providerRequestId,
        String failureCode,
        String failureMessage,
        Map<String, Object> metadata,
        OffsetDateTime respondedAt
) {
    public boolean approved() {
        return status == ProviderPaymentStatus.APPROVED;
    }

    public boolean declined() {
        return status == ProviderPaymentStatus.DECLINED;
    }
}
