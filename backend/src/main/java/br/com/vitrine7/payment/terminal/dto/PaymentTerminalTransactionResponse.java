package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PaymentTerminalTransactionResponse(
        UUID id,
        UUID paymentId,
        UUID checkoutSessionId,
        UUID idempotencyKey,
        String provider,
        String mode,
        String method,
        String status,
        Long amountCents,
        String providerTransactionId,
        String responseCode,
        String responseMessage,
        String errorCode,
        String errorMessage,
        UUID providerProfileId,
        String providerCodeSnapshot,
        String providerEnvironmentSnapshot,
        Long providerConfigurationVersion,
        String providerStatus,
        String providerReference,
        String providerRequestId,
        String providerFailureCode,
        String providerFailureMessage,
        Map<String, Object> providerMetadata,
        OffsetDateTime lastProviderSyncAt,
        UUID terminalDeviceId,
        UUID bridgeCommandId,
        String bridgeDeliveryStatus,
        OffsetDateTime sentAt,
        OffsetDateTime approvedAt,
        OffsetDateTime declinedAt,
        OffsetDateTime failedAt,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PaymentTerminalTransactionResponse from(
            PaymentTerminalTransactionEntity transaction
    ) {
        return new PaymentTerminalTransactionResponse(
                transaction.getId(),
                transaction.getPaymentId(),
                transaction.getCheckoutSessionId(),
                transaction.getIdempotencyKey(),
                transaction.getProvider().name(),
                transaction.getMode().name(),
                transaction.getMethod().name(),
                transaction.getStatus().name(),
                transaction.getAmountCents(),
                transaction.getProviderTransactionId(),
                transaction.getResponseCode(),
                transaction.getResponseMessage(),
                transaction.getErrorCode(),
                transaction.getErrorMessage(),
                transaction.getProviderProfileId(),
                transaction.getProviderCodeSnapshot().name(),
                transaction.getProviderEnvironmentSnapshot().name(),
                transaction.getProviderConfigurationVersion(),
                transaction.getProviderStatus() == null
                        ? null
                        : transaction.getProviderStatus().name(),
                transaction.getProviderReference(),
                transaction.getProviderRequestId(),
                transaction.getProviderFailureCode(),
                transaction.getProviderFailureMessage(),
                transaction.getProviderMetadata(),
                transaction.getLastProviderSyncAt(),
                transaction.getTerminalDeviceId(),
                transaction.getBridgeCommandId(),
                transaction.getBridgeDeliveryStatus(),
                transaction.getSentAt(),
                transaction.getApprovedAt(),
                transaction.getDeclinedAt(),
                transaction.getFailedAt(),
                transaction.getCreatedByUserId(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
