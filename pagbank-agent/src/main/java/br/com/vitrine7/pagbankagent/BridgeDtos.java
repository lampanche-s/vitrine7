package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class BridgeDtos {
    private BridgeDtos() {
    }

    public record PairRequest(
            String pairingCode,
            String providerCode,
            String platform,
            String agentVersion,
            String externalTerminalReference,
            JsonNode capabilities
    ) {
    }

    public record PairResponse(
            UUID deviceId,
            String deviceToken,
            OffsetDateTime pairedAt
    ) {
    }

    public record HeartbeatRequest(
            String providerCode,
            String platform,
            String agentVersion,
            String externalTerminalReference,
            JsonNode capabilities
    ) {
    }

    public record HeartbeatResponse(
            UUID deviceId,
            String status,
            OffsetDateTime lastSeenAt
    ) {
    }

    public record CommandDelivery(
            UUID commandId,
            UUID transactionId,
            String commandType,
            JsonNode payload,
            OffsetDateTime expiresAt,
            int deliveryAttempt
    ) {
    }

    public record Acknowledgement(
            UUID commandId,
            String status,
            OffsetDateTime acknowledgedAt
    ) {
    }

    public record ResultRequest(
            ProviderPaymentStatus status,
            String providerReference,
            String providerRequestId,
            String authorizationCode,
            String failureCode,
            String failureMessage,
            JsonNode metadata
    ) {
    }

    public record ResultResponse(
            UUID commandId,
            String commandStatus,
            boolean replayed
    ) {
    }
}
