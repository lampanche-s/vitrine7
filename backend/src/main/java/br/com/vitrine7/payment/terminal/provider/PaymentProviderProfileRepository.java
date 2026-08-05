package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentProviderProfileRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<PaymentProviderProfile> findAll() {
        return jdbcTemplate.query(
                """
                        SELECT *
                        FROM payment_provider_profiles
                        ORDER BY active DESC, provider_code ASC, created_at ASC
                        """,
                mapper()
        );
    }

    public Optional<PaymentProviderProfile> findById(UUID id) {
        return jdbcTemplate.query(
                        """
                                SELECT *
                                FROM payment_provider_profiles
                                WHERE id = ?
                                """,
                        mapper(),
                        id
                )
                .stream()
                .findFirst();
    }

    public Optional<PaymentProviderProfile> findActive() {
        return jdbcTemplate.query(
                        """
                                SELECT *
                                FROM payment_provider_profiles
                                WHERE active = TRUE
                                """,
                        mapper()
                )
                .stream()
                .findFirst();
    }

    public Optional<PaymentProviderProfile> findByProviderCode(
            PaymentProviderCode code
    ) {
        return jdbcTemplate.query(
                        """
                                SELECT *
                                FROM payment_provider_profiles
                                WHERE provider_code = ?
                                ORDER BY active DESC, created_at ASC
                                LIMIT 1
                                """,
                        mapper(),
                        code.name()
                )
                .stream()
                .findFirst();
    }

    public UUID create(
            PaymentProviderCode providerCode,
            String displayName,
            PaymentProviderEnvironment environment,
            boolean enabled,
            boolean active,
            String merchantReference,
            String terminalReference,
            Map<String, Object> publicConfiguration,
            PaymentProviderSecretsService.EncryptedCredentials credentials,
            List<String> credentialKeys,
            Long actorUserId
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO payment_provider_profiles (
                            id, provider_code, display_name, environment,
                            enabled, active, merchant_reference,
                            terminal_reference, public_configuration,
                            encrypted_credentials, credentials_nonce,
                            credentials_key_version, credential_keys,
                            configuration_version, created_by_user_id,
                            updated_by_user_id
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb),
                                ?, ?, ?, CAST(? AS jsonb), 1, ?, ?)
                        """,
                id,
                providerCode.name(),
                displayName,
                environment.name(),
                enabled,
                active,
                trimToNull(merchantReference),
                trimToNull(terminalReference),
                toJson(publicConfiguration),
                credentials == null ? null : credentials.ciphertext(),
                credentials == null ? null : credentials.nonce(),
                credentials == null ? null : credentials.keyVersion(),
                toJson(credentialKeys),
                actorUserId,
                actorUserId
        );
        return id;
    }

    public void update(
            UUID id,
            String displayName,
            PaymentProviderEnvironment environment,
            boolean enabled,
            String merchantReference,
            String terminalReference,
            Map<String, Object> publicConfiguration,
            PaymentProviderSecretsService.EncryptedCredentials credentials,
            List<String> credentialKeys,
            long expectedVersion,
            Long actorUserId
    ) {
        int updated;
        if (credentials == null) {
            updated = jdbcTemplate.update(
                    """
                            UPDATE payment_provider_profiles
                            SET display_name = ?,
                                environment = ?,
                                enabled = ?,
                                merchant_reference = ?,
                                terminal_reference = ?,
                                public_configuration = CAST(? AS jsonb),
                                configuration_version = configuration_version + 1,
                                updated_at = CURRENT_TIMESTAMP,
                                updated_by_user_id = ?,
                                version = version + 1
                            WHERE id = ?
                              AND version = ?
                            """,
                    displayName,
                    environment.name(),
                    enabled,
                    trimToNull(merchantReference),
                    trimToNull(terminalReference),
                    toJson(publicConfiguration),
                    actorUserId,
                    id,
                    expectedVersion
            );
        } else {
            updated = jdbcTemplate.update(
                    """
                            UPDATE payment_provider_profiles
                            SET display_name = ?,
                                environment = ?,
                                enabled = ?,
                                merchant_reference = ?,
                                terminal_reference = ?,
                                public_configuration = CAST(? AS jsonb),
                                encrypted_credentials = ?,
                                credentials_nonce = ?,
                                credentials_key_version = ?,
                                credential_keys = CAST(? AS jsonb),
                                configuration_version = configuration_version + 1,
                                updated_at = CURRENT_TIMESTAMP,
                                updated_by_user_id = ?,
                                version = version + 1
                            WHERE id = ?
                              AND version = ?
                            """,
                    displayName,
                    environment.name(),
                    enabled,
                    trimToNull(merchantReference),
                    trimToNull(terminalReference),
                    toJson(publicConfiguration),
                    credentials.ciphertext(),
                    credentials.nonce(),
                    credentials.keyVersion(),
                    toJson(credentialKeys),
                    actorUserId,
                    id,
                    expectedVersion
            );
        }

        if (updated == 0) {
            throw new NotFoundException(
                    "PAYMENT_PROVIDER_PROFILE_NOT_FOUND",
                    "Perfil de provider nao encontrado ou desatualizado."
            );
        }
    }

    public void lockActiveProfiles() {
        jdbcTemplate.queryForList(
                """
                        SELECT id
                        FROM payment_provider_profiles
                        WHERE active = TRUE
                        FOR UPDATE
                        """
        );
    }

    public void lockProfile(UUID id) {
        jdbcTemplate.queryForList(
                """
                        SELECT id
                        FROM payment_provider_profiles
                        WHERE id = ?
                        FOR UPDATE
                        """,
                id
        );
    }

    public void activate(UUID id, Long actorUserId) {
        jdbcTemplate.update(
                """
                        UPDATE payment_provider_profiles
                        SET active = FALSE,
                            updated_at = CURRENT_TIMESTAMP,
                            updated_by_user_id = ?,
                            version = version + 1
                        WHERE active = TRUE
                          AND id <> ?
                        """,
                actorUserId,
                id
        );

        jdbcTemplate.update(
                """
                        UPDATE payment_provider_profiles
                        SET active = TRUE,
                            updated_at = CURRENT_TIMESTAMP,
                            updated_by_user_id = ?,
                            version = version + 1
                        WHERE id = ?
                          AND active = FALSE
                        """,
                actorUserId,
                id
        );
    }

    private RowMapper<PaymentProviderProfile> mapper() {
        return (resultSet, rowNum) -> mapProfile(resultSet);
    }

    private PaymentProviderProfile mapProfile(
            ResultSet rs
    ) throws SQLException {
        return new PaymentProviderProfile(
                rs.getObject("id", UUID.class),
                PaymentProviderCode.valueOf(rs.getString("provider_code")),
                rs.getString("display_name"),
                PaymentProviderEnvironment.valueOf(rs.getString("environment")),
                rs.getBoolean("enabled"),
                rs.getBoolean("active"),
                rs.getString("merchant_reference"),
                rs.getString("terminal_reference"),
                parseJson(rs.getString("public_configuration")),
                rs.getBytes("encrypted_credentials") != null,
                parseKeys(rs.getString("credential_keys")),
                rs.getLong("configuration_version"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                nullableLong(rs, "created_by_user_id"),
                nullableLong(rs, "updated_by_user_id"),
                rs.getLong("version")
        );
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private List<String> parseKeys(String json) {
        if (json == null || json.equals("[]")) {
            return List.of();
        }
        String raw = json
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "");
        if (raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
