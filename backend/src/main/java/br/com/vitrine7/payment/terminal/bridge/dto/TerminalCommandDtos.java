package br.com.vitrine7.payment.terminal.bridge.dto;

import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TerminalCommandDtos {
    private TerminalCommandDtos() {}

    public record Delivery(
            UUID commandId, UUID transactionId, String commandType,
            JsonNode payload, OffsetDateTime expiresAt, int deliveryAttempt
    ) {}

    public record Acknowledgement(UUID commandId, String status, OffsetDateTime acknowledgedAt) {}

    public record ResultRequest(
            @NotNull ProviderPaymentStatus status,
            @Size(max = 120) String providerReference,
            @Size(max = 120) String providerRequestId,
            @Size(max = 80) String authorizationCode,
            @Size(max = 80) String failureCode,
            @Size(max = 255) String failureMessage,
            @NotNull JsonNode metadata
    ) {}

    public record ResultResponse(UUID commandId, String commandStatus, boolean replayed) {}
}
