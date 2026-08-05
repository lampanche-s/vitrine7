package br.com.vitrine7.payment.terminal.bridge.dto;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TerminalDeviceDtos {
    private TerminalDeviceDtos() {}

    public record CreateRequest(
            UUID providerProfileId,
            @NotNull PaymentProviderCode providerCode,
            @NotBlank @Size(max = 120) String displayName,
            @Size(max = 120) String externalTerminalReference,
            @NotBlank @Size(max = 60) String platform
    ) {}

    public record Response(
            UUID id, UUID providerProfileId, PaymentProviderCode providerCode,
            String displayName, String externalTerminalReference, String status,
            String platform, String agentVersion, JsonNode capabilities,
            OffsetDateTime pairedAt, OffsetDateTime lastSeenAt,
            OffsetDateTime revokedAt, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, long version
    ) {}

    public record PairingCodeResponse(UUID deviceId, String pairingCode, OffsetDateTime expiresAt) {}

    public record PairRequest(
            @NotBlank @Size(max = 128) String pairingCode,
            @NotNull PaymentProviderCode providerCode,
            @NotBlank @Size(max = 60) String platform,
            @NotBlank @Size(max = 60) String agentVersion,
            @Size(max = 120) String externalTerminalReference,
            @NotNull JsonNode capabilities
    ) {}

    public record PairResponse(UUID deviceId, String deviceToken, OffsetDateTime pairedAt) {}

    public record HeartbeatRequest(
            @NotNull PaymentProviderCode providerCode,
            @NotBlank @Size(max = 60) String platform,
            @NotBlank @Size(max = 60) String agentVersion,
            @Size(max = 120) String externalTerminalReference,
            @NotNull JsonNode capabilities
    ) {}

    public record HeartbeatResponse(UUID deviceId, String status, OffsetDateTime lastSeenAt) {}
}
