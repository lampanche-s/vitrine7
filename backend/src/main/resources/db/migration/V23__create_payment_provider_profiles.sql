CREATE TABLE payment_provider_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code VARCHAR(30) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    environment VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    merchant_reference VARCHAR(120),
    terminal_reference VARCHAR(120),
    public_configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    encrypted_credentials BYTEA,
    credentials_nonce BYTEA,
    credentials_key_version INTEGER,
    credential_keys JSONB NOT NULL DEFAULT '[]'::jsonb,
    configuration_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT,
    updated_by_user_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_provider_profiles_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_payment_provider_profiles_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_payment_provider_profiles_provider
        CHECK (provider_code IN ('SIMULATOR', 'STONE', 'PAGBANK', 'CIELO', 'TON', 'C6')),
    CONSTRAINT ck_payment_provider_profiles_environment
        CHECK (environment IN ('LOCAL', 'SANDBOX', 'PRODUCTION')),
    CONSTRAINT ck_payment_provider_profiles_public_configuration_object
        CHECK (jsonb_typeof(public_configuration) = 'object'),
    CONSTRAINT ck_payment_provider_profiles_credential_keys_array
        CHECK (jsonb_typeof(credential_keys) = 'array'),
    CONSTRAINT ck_payment_provider_profiles_credentials_complete CHECK (
        (
            encrypted_credentials IS NULL
            AND credentials_nonce IS NULL
            AND credentials_key_version IS NULL
        )
        OR (
            encrypted_credentials IS NOT NULL
            AND credentials_nonce IS NOT NULL
            AND credentials_key_version IS NOT NULL
        )
    ),
    CONSTRAINT ck_payment_provider_profiles_active_enabled
        CHECK (active = FALSE OR enabled = TRUE),
    CONSTRAINT ck_payment_provider_profiles_configuration_version
        CHECK (configuration_version >= 1),
    CONSTRAINT ck_payment_provider_profiles_display_name_length
        CHECK (char_length(trim(display_name)) BETWEEN 1 AND 120),
    CONSTRAINT ck_payment_provider_profiles_references_length CHECK (
        (merchant_reference IS NULL OR char_length(merchant_reference) <= 120)
        AND (terminal_reference IS NULL OR char_length(terminal_reference) <= 120)
    ),
    CONSTRAINT ck_payment_provider_profiles_public_configuration_size
        CHECK (pg_column_size(public_configuration) <= 8192)
);

CREATE UNIQUE INDEX uq_payment_provider_profiles_active
    ON payment_provider_profiles (active)
    WHERE active = TRUE;

CREATE INDEX idx_payment_provider_profiles_provider
    ON payment_provider_profiles (provider_code);

CREATE INDEX idx_payment_provider_profiles_enabled
    ON payment_provider_profiles (enabled);

INSERT INTO payment_provider_profiles (
    provider_code,
    display_name,
    environment,
    enabled,
    active,
    public_configuration,
    configuration_version
)
SELECT
    'SIMULATOR',
    'Terminal Simulado',
    'LOCAL',
    TRUE,
    TRUE,
    jsonb_build_object(
        'simulatedOutcome',
        COALESCE(settings.simulated_outcome, 'APPROVED')
    ),
    1
FROM payment_terminal_settings settings
WHERE settings.id = 1
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_payment_terminal_transactions_provider'
    ) THEN
        ALTER TABLE payment_terminal_transactions
            DROP CONSTRAINT ck_payment_terminal_transactions_provider;
    END IF;
END $$;

ALTER TABLE payment_terminal_transactions
    ADD CONSTRAINT ck_payment_terminal_transactions_provider
        CHECK (provider IN ('SIMULATOR', 'STONE', 'PAGBANK', 'CIELO', 'TON', 'C6'));

ALTER TABLE payment_terminal_transactions
    ADD COLUMN provider_profile_id UUID,
    ADD COLUMN provider_code_snapshot VARCHAR(30),
    ADD COLUMN provider_environment_snapshot VARCHAR(30),
    ADD COLUMN provider_configuration_version BIGINT,
    ADD COLUMN provider_status VARCHAR(30),
    ADD COLUMN provider_reference VARCHAR(120),
    ADD COLUMN provider_request_id VARCHAR(120),
    ADD COLUMN provider_failure_code VARCHAR(80),
    ADD COLUMN provider_failure_message VARCHAR(255),
    ADD COLUMN provider_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN last_provider_sync_at TIMESTAMPTZ;

UPDATE payment_terminal_transactions transaction
SET
    provider_profile_id = CASE
        WHEN transaction.mode = 'SIMULATED' THEN profile.id
        ELSE NULL
    END,
    provider_code_snapshot = CASE
        WHEN transaction.mode = 'SIMULATED' THEN 'SIMULATOR'
        ELSE transaction.provider
    END,
    provider_environment_snapshot = CASE
        WHEN transaction.mode = 'SIMULATED' THEN 'LOCAL'
        ELSE 'PRODUCTION'
    END,
    provider_configuration_version = CASE
        WHEN transaction.mode = 'SIMULATED' THEN profile.configuration_version
        ELSE 1
    END,
    provider_status = CASE transaction.status
        WHEN 'SENT' THEN 'PROCESSING'
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'DECLINED' THEN 'DECLINED'
        WHEN 'ERROR' THEN 'ERROR'
        ELSE 'UNKNOWN'
    END,
    provider_reference = transaction.provider_transaction_id,
    provider_request_id = transaction.provider_transaction_id,
    provider_failure_code = COALESCE(transaction.error_code, CASE WHEN transaction.status = 'DECLINED' THEN transaction.response_code ELSE NULL END),
    provider_failure_message = COALESCE(transaction.error_message, CASE WHEN transaction.status = 'DECLINED' THEN transaction.response_message ELSE NULL END),
    last_provider_sync_at = COALESCE(transaction.approved_at, transaction.declined_at, transaction.failed_at, transaction.sent_at)
FROM payment_provider_profiles profile
WHERE profile.provider_code = 'SIMULATOR'
  AND transaction.provider_code_snapshot IS NULL;

ALTER TABLE payment_terminal_transactions
    ALTER COLUMN provider_code_snapshot SET NOT NULL,
    ALTER COLUMN provider_environment_snapshot SET NOT NULL,
    ALTER COLUMN provider_configuration_version SET NOT NULL,
    ADD CONSTRAINT fk_payment_terminal_transactions_provider_profile
        FOREIGN KEY (provider_profile_id) REFERENCES payment_provider_profiles (id),
    ADD CONSTRAINT ck_payment_terminal_transactions_provider_snapshot
        CHECK (provider_code_snapshot IN ('SIMULATOR', 'STONE', 'PAGBANK', 'CIELO', 'TON', 'C6')),
    ADD CONSTRAINT ck_payment_terminal_transactions_provider_environment_snapshot
        CHECK (provider_environment_snapshot IN ('LOCAL', 'SANDBOX', 'PRODUCTION')),
    ADD CONSTRAINT ck_payment_terminal_transactions_provider_configuration_version
        CHECK (provider_configuration_version >= 1),
    ADD CONSTRAINT ck_payment_terminal_transactions_provider_status
        CHECK (provider_status IS NULL OR provider_status IN ('CREATED', 'PENDING', 'PROCESSING', 'APPROVED', 'DECLINED', 'CANCELLED', 'ERROR', 'UNKNOWN')),
    ADD CONSTRAINT ck_payment_terminal_transactions_provider_metadata_object
        CHECK (jsonb_typeof(provider_metadata) = 'object');

CREATE INDEX idx_payment_terminal_transactions_provider_profile
    ON payment_terminal_transactions (provider_profile_id)
    WHERE provider_profile_id IS NOT NULL;

CREATE INDEX idx_payment_terminal_transactions_provider_snapshot
    ON payment_terminal_transactions (provider_code_snapshot, created_at DESC);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_payment_terminal_settings_provider'
    ) THEN
        ALTER TABLE payment_terminal_settings
            DROP CONSTRAINT ck_payment_terminal_settings_provider;
    END IF;
END $$;

ALTER TABLE payment_terminal_settings
    ADD CONSTRAINT ck_payment_terminal_settings_provider
        CHECK (provider IS NULL OR provider IN ('SIMULATOR', 'STONE', 'PAGBANK', 'CIELO', 'TON', 'C6'));
