package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.payment.terminal.bridge.dto.TerminalDeviceDtos;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TerminalDeviceRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public UUID create(TerminalDeviceDtos.CreateRequest request, Long actorId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payment_terminal_devices
                  (id, provider_profile_id, provider_code, display_name,
                   external_terminal_reference, platform, created_by_user_id, updated_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, request.providerProfileId(), request.providerCode().name(),
                request.displayName().trim(), trim(request.externalTerminalReference()),
                request.platform().trim(), actorId, actorId);
        return id;
    }

    public List<TerminalDeviceDtos.Response> findAll(OffsetDateTime onlineCutoff) {
        return jdbc.query(selectSql() + " ORDER BY created_at DESC", (rs, n) -> map(rs, onlineCutoff));
    }

    public TerminalDeviceDtos.Response find(UUID id, OffsetDateTime onlineCutoff) {
        return jdbc.query(selectSql() + " WHERE id = ?", (rs, n) -> map(rs, onlineCutoff), id)
                .stream().findFirst().orElse(null);
    }

    public DeviceAuth findAuthByTokenHash(byte[] suppliedHash) {
        for (DeviceAuth candidate : jdbc.query("""
                SELECT id, provider_code, status, token_hash
                FROM payment_terminal_devices WHERE token_hash IS NOT NULL
                """, (rs, n) -> new DeviceAuth(
                rs.getObject("id", UUID.class),
                PaymentProviderCode.valueOf(rs.getString("provider_code")),
                TerminalDeviceStatus.valueOf(rs.getString("status")),
                rs.getBytes("token_hash")))) {
            if (java.security.MessageDigest.isEqual(candidate.tokenHash(), suppliedHash)) return candidate;
        }
        return null;
    }

    public List<PairingCandidate> findPairingCandidates() {
        return jdbc.query("""
                SELECT id, device_id, code_hash, expires_at, used_at
                FROM payment_terminal_pairing_codes
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day'
                """, (rs, n) -> new PairingCandidate(
                rs.getObject("id", UUID.class), rs.getObject("device_id", UUID.class),
                rs.getBytes("code_hash"), rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("used_at", OffsetDateTime.class)));
    }

    public DevicePairingData lockPairing(UUID pairingId) {
        return jdbc.query("""
                SELECT pc.id pairing_id, pc.device_id, pc.code_hash, pc.expires_at, pc.used_at,
                       d.provider_code, d.status
                FROM payment_terminal_pairing_codes pc
                JOIN payment_terminal_devices d ON d.id = pc.device_id
                WHERE pc.id = ? FOR UPDATE OF pc, d
                """, (rs, n) -> new DevicePairingData(
                rs.getObject("pairing_id", UUID.class), rs.getObject("device_id", UUID.class),
                rs.getBytes("code_hash"), rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("used_at", OffsetDateTime.class),
                PaymentProviderCode.valueOf(rs.getString("provider_code")),
                TerminalDeviceStatus.valueOf(rs.getString("status"))), pairingId)
                .stream().findFirst().orElse(null);
    }

    public void invalidatePairingCodes(UUID deviceId, OffsetDateTime now) {
        jdbc.update("""
                UPDATE payment_terminal_pairing_codes SET used_at = ?
                WHERE device_id = ? AND used_at IS NULL
                """, now, deviceId);
    }

    public UUID insertPairingCode(UUID deviceId, byte[] hash, OffsetDateTime expiresAt, Long actorId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payment_terminal_pairing_codes
                  (id, device_id, code_hash, expires_at, created_by_user_id)
                VALUES (?, ?, ?, ?, ?)
                """, id, deviceId, hash, expiresAt, actorId);
        return id;
    }

    public void pair(DevicePairingData data, byte[] tokenHash,
                     TerminalDeviceDtos.PairRequest request, OffsetDateTime now) {
        jdbc.update("UPDATE payment_terminal_pairing_codes SET used_at = ? WHERE id = ?", now, data.pairingId());
        jdbc.update("""
                UPDATE payment_terminal_devices
                SET status='ACTIVE', platform=?, agent_version=?, capabilities=CAST(? AS jsonb),
                    external_terminal_reference=COALESCE(?, external_terminal_reference),
                    token_hash=?, paired_at=?, last_seen_at=?, revoked_at=NULL,
                    updated_at=?, version=version+1
                WHERE id=?
                """, request.platform().trim(), request.agentVersion().trim(), json(request.capabilities()),
                trim(request.externalTerminalReference()), tokenHash, now, now, now, data.deviceId());
    }

    public void heartbeat(UUID deviceId, TerminalDeviceDtos.HeartbeatRequest request, OffsetDateTime now) {
        jdbc.update("""
                UPDATE payment_terminal_devices
                SET status='ACTIVE', platform=?, agent_version=?, capabilities=CAST(? AS jsonb),
                    external_terminal_reference=COALESCE(?, external_terminal_reference),
                    last_seen_at=?, updated_at=?, version=version+1
                WHERE id=? AND status <> 'REVOKED'
                """, request.platform().trim(), request.agentVersion().trim(), json(request.capabilities()),
                trim(request.externalTerminalReference()), now, now, deviceId);
    }

    public DeviceSelection selectAvailable(PaymentProviderCode provider, UUID profileId,
                                           String terminalReference, String paymentMethod,
                                           OffsetDateTime cutoff) {
        return jdbc.query("""
                SELECT id, provider_code FROM payment_terminal_devices
                WHERE provider_code=? AND status='ACTIVE' AND token_hash IS NOT NULL
                  AND last_seen_at >= ?
                  AND (provider_profile_id IS NULL OR provider_profile_id = ?)
                  AND (CAST(? AS varchar) IS NULL OR external_terminal_reference = ?)
                  AND (
                      capabilities->'paymentMethods' IS NULL
                      OR jsonb_exists(
                          capabilities->'paymentMethods',
                          ?
                      )
                  )
                ORDER BY CASE WHEN provider_profile_id = ? THEN 0 ELSE 1 END, last_seen_at DESC
                LIMIT 1
                """, (rs, n) -> new DeviceSelection(rs.getObject("id", UUID.class),
                        PaymentProviderCode.valueOf(rs.getString("provider_code"))),
                provider.name(), cutoff, profileId, terminalReference, terminalReference,
                paymentMethod, profileId)
                .stream().findFirst().orElse(null);
    }

    public boolean isAvailable(
            UUID deviceId,
            OffsetDateTime onlineCutoff
    ) {
        Boolean available = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM payment_terminal_devices
                    WHERE id = ?
                      AND provider_code = 'PAGBANK'
                      AND status = 'ACTIVE'
                      AND token_hash IS NOT NULL
                      AND last_seen_at >= ?
                )
                """,
                Boolean.class,
                deviceId,
                onlineCutoff
        );

        return Boolean.TRUE.equals(available);
    }

    public OperationalSnapshot pagBankOperationalSnapshot() {
        return jdbc.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE status = 'ACTIVE') AS active,
                       count(*) FILTER (WHERE status = 'REVOKED') AS revoked,
                       max(last_seen_at) FILTER (WHERE status = 'ACTIVE') AS last_seen_at,
                       (array_agg(agent_version ORDER BY last_seen_at DESC NULLS LAST)
                           FILTER (WHERE status = 'ACTIVE'))[1] AS agent_version
                FROM payment_terminal_devices
                WHERE provider_code = 'PAGBANK'
                """, (rs, row) -> new OperationalSnapshot(
                rs.getLong("total"),
                rs.getLong("active"),
                rs.getLong("revoked"),
                rs.getObject("last_seen_at", OffsetDateTime.class),
                rs.getString("agent_version")
        ));
    }

    public boolean revoke(UUID id, Long actorId, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE payment_terminal_devices
                SET status='REVOKED', revoked_at=?, updated_at=?, updated_by_user_id=?, version=version+1
                WHERE id=? AND status <> 'REVOKED'
                """, now, now, actorId, id) > 0;
    }

    private String selectSql() {
        return """
                SELECT id, provider_profile_id, provider_code, display_name,
                       external_terminal_reference, status, platform, agent_version,
                       capabilities::text capabilities, paired_at, last_seen_at, revoked_at,
                       created_at, updated_at, version
                FROM payment_terminal_devices
                """;
    }

    private TerminalDeviceDtos.Response map(ResultSet rs, OffsetDateTime cutoff) throws SQLException {
        String persisted = rs.getString("status");
        OffsetDateTime lastSeen = rs.getObject("last_seen_at", OffsetDateTime.class);
        String effective = persisted.equals("ACTIVE") && (lastSeen == null || lastSeen.isBefore(cutoff))
                ? "OFFLINE" : persisted;
        return new TerminalDeviceDtos.Response(rs.getObject("id", UUID.class),
                rs.getObject("provider_profile_id", UUID.class),
                PaymentProviderCode.valueOf(rs.getString("provider_code")), rs.getString("display_name"),
                rs.getString("external_terminal_reference"), effective, rs.getString("platform"),
                rs.getString("agent_version"), parse(rs.getString("capabilities")),
                rs.getObject("paired_at", OffsetDateTime.class), lastSeen,
                rs.getObject("revoked_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("version"));
    }

    private JsonNode parse(String value) { try { return mapper.readTree(value); } catch (Exception e) { return mapper.createObjectNode(); } }
    private String json(JsonNode value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { return "{}"; } }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record DeviceAuth(UUID id, PaymentProviderCode providerCode, TerminalDeviceStatus status, byte[] tokenHash) {}
    public record PairingCandidate(UUID id, UUID deviceId, byte[] codeHash, OffsetDateTime expiresAt, OffsetDateTime usedAt) {}
    public record DevicePairingData(UUID pairingId, UUID deviceId, byte[] codeHash, OffsetDateTime expiresAt,
                                    OffsetDateTime usedAt, PaymentProviderCode providerCode, TerminalDeviceStatus status) {}
    public record DeviceSelection(UUID id, PaymentProviderCode providerCode) {}
    public record OperationalSnapshot(
            long total,
            long active,
            long revoked,
            OffsetDateTime lastSeenAt,
            String agentVersion
    ) {}
}
