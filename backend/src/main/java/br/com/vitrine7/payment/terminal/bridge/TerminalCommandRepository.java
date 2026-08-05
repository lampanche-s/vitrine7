package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.payment.terminal.bridge.dto.TerminalCommandDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TerminalCommandRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public CommandTransaction findTransactionByPayment(UUID paymentId) {
        return jdbc.query("""
                SELECT t.id, t.payment_id, t.checkout_session_id, t.idempotency_key,
                       t.provider_code_snapshot, t.provider_environment_snapshot,
                       t.provider_configuration_version, t.provider_profile_id,
                       t.method, t.amount_cents, t.created_by_user_id,
                       p.merchant_reference, p.terminal_reference
                FROM payment_terminal_transactions t
                LEFT JOIN payment_provider_profiles p ON p.id=t.provider_profile_id
                WHERE t.payment_id=?
                """, (rs, n) -> new CommandTransaction(
                rs.getObject("id", UUID.class), rs.getObject("payment_id", UUID.class),
                rs.getObject("checkout_session_id", UUID.class), rs.getObject("idempotency_key", UUID.class),
                rs.getString("provider_code_snapshot"), rs.getString("provider_environment_snapshot"),
                rs.getLong("provider_configuration_version"), rs.getObject("provider_profile_id", UUID.class),
                rs.getString("method"), rs.getLong("amount_cents"), rs.getLong("created_by_user_id"),
                rs.getString("merchant_reference"), rs.getString("terminal_reference")), paymentId)
                .stream().findFirst().orElse(null);
    }

    public CommandTransaction findTransaction(
            UUID transactionId
    ) {
        return jdbc.query("""
                SELECT t.id,
                       t.payment_id,
                       t.checkout_session_id,
                       t.idempotency_key,
                       t.provider_code_snapshot,
                       t.provider_environment_snapshot,
                       t.provider_configuration_version,
                       t.provider_profile_id,
                       t.method,
                       t.amount_cents,
                       t.created_by_user_id,
                       p.merchant_reference,
                       p.terminal_reference
                FROM payment_terminal_transactions t
                LEFT JOIN payment_provider_profiles p
                  ON p.id = t.provider_profile_id
                WHERE t.id = ?
                """,
                (rs, number) -> new CommandTransaction(
                        rs.getObject("id", UUID.class),
                        rs.getObject(
                                "payment_id",
                                UUID.class
                        ),
                        rs.getObject(
                                "checkout_session_id",
                                UUID.class
                        ),
                        rs.getObject(
                                "idempotency_key",
                                UUID.class
                        ),
                        rs.getString(
                                "provider_code_snapshot"
                        ),
                        rs.getString(
                                "provider_environment_snapshot"
                        ),
                        rs.getLong(
                                "provider_configuration_version"
                        ),
                        rs.getObject(
                                "provider_profile_id",
                                UUID.class
                        ),
                        rs.getString("method"),
                        rs.getLong("amount_cents"),
                        rs.getLong("created_by_user_id"),
                        rs.getString(
                                "merchant_reference"
                        ),
                        rs.getString(
                                "terminal_reference"
                        )
                ),
                transactionId
        ).stream().findFirst().orElse(null);
    }

    public UUID create(UUID commandId, UUID deviceId, CommandTransaction tx, JsonNode payload, OffsetDateTime expiresAt) {
        jdbc.update("""
                INSERT INTO payment_terminal_commands
                  (id, device_id, terminal_transaction_id, command_type, status,
                   idempotency_key, payload, result, available_at, expires_at)
                VALUES (?, ?, ?, 'INITIATE_PAYMENT', 'QUEUED', ?, CAST(? AS jsonb), '{}'::jsonb,
                        CURRENT_TIMESTAMP, ?)
                """, commandId, deviceId, tx.id(), tx.id(), json(payload), expiresAt);
        jdbc.update("""
                UPDATE payment_terminal_transactions
                SET terminal_device_id=?, bridge_command_id=?, bridge_delivery_status='QUEUED', updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND bridge_command_id IS NULL
                """, deviceId, commandId, tx.id());
        return commandId;
    }

    public UUID createQuery(
            UUID commandId,
            UUID deviceId,
            CommandTransaction transaction,
            JsonNode payload,
            OffsetDateTime expiresAt
    ) {
        jdbc.update("""
                INSERT INTO payment_terminal_commands
                  (
                    id,
                    device_id,
                    terminal_transaction_id,
                    command_type,
                    status,
                    idempotency_key,
                    payload,
                    result,
                    available_at,
                    expires_at
                  )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'QUERY_PAYMENT',
                    'QUEUED',
                    ?,
                    CAST(? AS jsonb),
                    '{}'::jsonb,
                    CURRENT_TIMESTAMP,
                    ?
                )
                ON CONFLICT DO NOTHING
                """,
                commandId,
                deviceId,
                transaction.id(),
                commandId,
                json(payload),
                expiresAt
        );

        return commandId;
    }

    public UUID createReversal(
            UUID commandId,
            UUID deviceId,
            UUID transactionId,
            JsonNode payload,
            OffsetDateTime expiresAt
    ) {
        jdbc.update("""
                INSERT INTO payment_terminal_commands
                  (
                    id,
                    device_id,
                    terminal_transaction_id,
                    command_type,
                    status,
                    idempotency_key,
                    payload,
                    result,
                    available_at,
                    expires_at
                  )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'REVERSE_PAYMENT',
                    'QUEUED',
                    ?,
                    CAST(? AS jsonb),
                    '{}'::jsonb,
                    CURRENT_TIMESTAMP,
                    ?
                )
                """,
                commandId,
                deviceId,
                transactionId,
                commandId,
                json(payload),
                expiresAt
        );

        return commandId;
    }

    public ReversalContext findReversalContext(
            UUID commandId
    ) {
        return jdbc.query("""
                SELECT
                    (payload ->> 'requestedByUserId')::bigint
                        AS requested_by_user_id,
                    payload ->> 'reason'
                        AS reason
                FROM payment_terminal_commands
                WHERE id = ?
                  AND command_type = 'REVERSE_PAYMENT'
                """,
                (rs, number) -> new ReversalContext(
                        rs.getLong(
                                "requested_by_user_id"
                        ),
                        rs.getString("reason")
                ),
                commandId
        ).stream().findFirst().orElse(null);
    }

    public CommandSnapshot findByTransaction(UUID transactionId) {
        return jdbc.query(snapshotSql() + " WHERE terminal_transaction_id=? AND command_type='INITIATE_PAYMENT'",
                (rs, n) -> snapshot(rs), transactionId).stream().findFirst().orElse(null);
    }

    public CommandSnapshot find(UUID commandId) {
        return jdbc.query(snapshotSql() + " WHERE id=?", (rs, n) -> snapshot(rs), commandId)
                .stream().findFirst().orElse(null);
    }

    public TerminalCommandDtos.Delivery reserveNext(UUID deviceId, OffsetDateTime now) {
        return jdbc.query("""
                WITH candidate AS (
                    SELECT id FROM payment_terminal_commands
                    WHERE device_id=? AND status IN ('QUEUED','DELIVERED')
                      AND available_at <= ? AND expires_at > ?
                    ORDER BY available_at, created_at
                    FOR UPDATE SKIP LOCKED LIMIT 1
                )
                UPDATE payment_terminal_commands c
                SET status='DELIVERED', delivered_at=COALESCE(delivered_at, ?),
                    delivery_attempts=delivery_attempts+1, available_at=? + INTERVAL '5 seconds',
                    updated_at=?, version=version+1
                FROM candidate WHERE c.id=candidate.id
                RETURNING c.id, c.terminal_transaction_id, c.command_type, c.payload::text,
                          c.expires_at, c.delivery_attempts
                """, (rs, n) -> new TerminalCommandDtos.Delivery(
                rs.getObject("id", UUID.class), rs.getObject("terminal_transaction_id", UUID.class),
                rs.getString("command_type"), parse(rs.getString("payload")),
                rs.getObject("expires_at", OffsetDateTime.class), rs.getInt("delivery_attempts")),
                deviceId, now, now, now, now, now).stream().findFirst().orElse(null);
    }

    public void mirrorDelivery(UUID commandId, String status) {
        jdbc.update("""
                UPDATE payment_terminal_transactions SET bridge_delivery_status=?, updated_at=CURRENT_TIMESTAMP
                WHERE bridge_command_id=?
                """, status, commandId);
    }

    public void mirrorTransactionDelivery(
            UUID transactionId,
            String status
    ) {
        jdbc.update("""
                UPDATE payment_terminal_transactions
                SET bridge_delivery_status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status,
                transactionId
        );
    }

    public int acknowledge(UUID commandId, UUID deviceId, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE payment_terminal_commands
                SET status='ACKNOWLEDGED', acknowledged_at=COALESCE(acknowledged_at, ?),
                    updated_at=?, version=version+1
                WHERE id=? AND device_id=? AND status='DELIVERED' AND expires_at>?
                """, now, now, commandId, deviceId, now);
    }

    public int finish(
            UUID commandId,
            UUID deviceId,
            String status,
            JsonNode result,
            String failureCode,
            String failureMessage,
            OffsetDateTime now
    ) {
        return jdbc.update("""
                UPDATE payment_terminal_commands
                SET status=?,
                    result=CAST(? AS jsonb),
                    completed_at=?,
                    failure_code=?,
                    failure_message=?,
                    updated_at=?,
                    version=version+1
                WHERE id=?
                  AND device_id=?
                  AND status IN (
                      'DELIVERED',
                      'ACKNOWLEDGED',
                      'EXPIRED'
                  )
                """,
                status,
                json(result),
                now,
                failureCode,
                failureMessage,
                now,
                commandId,
                deviceId
        );
    }

    public int recordProgress(UUID commandId, UUID deviceId, JsonNode result, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE payment_terminal_commands
                SET status='ACKNOWLEDGED', acknowledged_at=COALESCE(acknowledged_at, ?),
                    result=CAST(? AS jsonb), updated_at=?, version=version+1
                WHERE id=? AND device_id=? AND status IN ('DELIVERED','ACKNOWLEDGED')
                  AND expires_at > ?
                """, now, json(result), now, commandId, deviceId, now);
    }

    public List<CommandSnapshot> expireDue(OffsetDateTime now) {
        return jdbc.query("""
                UPDATE payment_terminal_commands
                SET status='EXPIRED', failure_code='PAYMENT_TERMINAL_COMMAND_EXPIRED',
                    failure_message='Comando expirado.', updated_at=?, version=version+1
                WHERE id IN (
                    SELECT id FROM payment_terminal_commands
                    WHERE status IN ('QUEUED','DELIVERED','ACKNOWLEDGED') AND expires_at<=?
                    FOR UPDATE SKIP LOCKED LIMIT 100
                )
                RETURNING id, device_id, terminal_transaction_id, command_type, status,
                          result::text, expires_at, failure_code, failure_message
                """, (rs, n) -> snapshot(rs), now, now);
    }

    private String snapshotSql() {
        return """
                SELECT id, device_id, terminal_transaction_id, command_type, status,
                       result::text result, expires_at, failure_code, failure_message
                FROM payment_terminal_commands
                """;
    }

    private CommandSnapshot snapshot(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CommandSnapshot(rs.getObject("id", UUID.class), rs.getObject("device_id", UUID.class),
                rs.getObject("terminal_transaction_id", UUID.class), rs.getString("command_type"),
                rs.getString("status"), parse(rs.getString("result")),
                rs.getObject("expires_at", OffsetDateTime.class), rs.getString("failure_code"),
                rs.getString("failure_message"));
    }
    private String json(JsonNode node) { try { return mapper.writeValueAsString(node); } catch (Exception e) { return "{}"; } }
    private JsonNode parse(String text) { try { return mapper.readTree(text == null ? "{}" : text); } catch (Exception e) { return mapper.createObjectNode(); } }

    public record CommandTransaction(UUID id, UUID paymentId, UUID checkoutId, UUID paymentIdempotencyKey,
                                     String providerCode, String environment, long configurationVersion,
                                     UUID profileId, String method, long amountCents, Long actorUserId,
                                     String merchantReference, String terminalReference) {}
    public record CommandSnapshot(UUID id, UUID deviceId, UUID transactionId, String type, String status,
                                  JsonNode result, OffsetDateTime expiresAt, String failureCode, String failureMessage) {}

    public record ReversalContext(
            Long requestedByUserId,
            String reason
    ) {
    }
}
