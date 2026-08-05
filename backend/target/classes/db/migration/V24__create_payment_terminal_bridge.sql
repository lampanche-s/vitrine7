CREATE TABLE payment_terminal_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_profile_id UUID,
    provider_code VARCHAR(30) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    external_terminal_reference VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAIRING',
    platform VARCHAR(60) NOT NULL,
    agent_version VARCHAR(60),
    capabilities JSONB NOT NULL DEFAULT '{}'::jsonb,
    token_hash BYTEA,
    paired_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT,
    updated_by_user_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_terminal_devices_profile FOREIGN KEY (provider_profile_id) REFERENCES payment_provider_profiles (id),
    CONSTRAINT fk_payment_terminal_devices_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_payment_terminal_devices_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_payment_terminal_devices_provider CHECK (provider_code IN ('SIMULATOR','STONE','PAGBANK','CIELO','TON','C6')),
    CONSTRAINT ck_payment_terminal_devices_status CHECK (status IN ('PENDING_PAIRING','ACTIVE','OFFLINE','REVOKED')),
    CONSTRAINT ck_payment_terminal_devices_capabilities_object CHECK (jsonb_typeof(capabilities) = 'object'),
    CONSTRAINT ck_payment_terminal_devices_active_token CHECK (status <> 'ACTIVE' OR token_hash IS NOT NULL),
    CONSTRAINT ck_payment_terminal_devices_revoked CHECK (status <> 'REVOKED' OR revoked_at IS NOT NULL),
    CONSTRAINT ck_payment_terminal_devices_lengths CHECK (
        char_length(trim(display_name)) BETWEEN 1 AND 120 AND
        char_length(trim(platform)) BETWEEN 1 AND 60 AND
        (agent_version IS NULL OR char_length(agent_version) <= 60) AND
        (external_terminal_reference IS NULL OR char_length(external_terminal_reference) <= 120)
    ),
    CONSTRAINT ck_payment_terminal_devices_capabilities_size CHECK (pg_column_size(capabilities) <= 8192)
);

CREATE UNIQUE INDEX uq_payment_terminal_devices_external_reference
    ON payment_terminal_devices (provider_code, external_terminal_reference)
    WHERE external_terminal_reference IS NOT NULL;
CREATE INDEX idx_payment_terminal_devices_available
    ON payment_terminal_devices (provider_code, provider_profile_id, status, last_seen_at DESC);

CREATE TABLE payment_terminal_pairing_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL,
    code_hash BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT,
    CONSTRAINT fk_payment_terminal_pairing_device FOREIGN KEY (device_id) REFERENCES payment_terminal_devices (id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_terminal_pairing_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_payment_terminal_pairing_expiry CHECK (expires_at > created_at),
    CONSTRAINT uq_payment_terminal_pairing_hash UNIQUE (code_hash)
);
CREATE INDEX idx_payment_terminal_pairing_active ON payment_terminal_pairing_codes (device_id, expires_at) WHERE used_at IS NULL;

CREATE TABLE payment_terminal_commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL,
    terminal_transaction_id UUID NOT NULL,
    command_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    idempotency_key UUID NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    result JSONB NOT NULL DEFAULT '{}'::jsonb,
    delivery_attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    failure_code VARCHAR(80),
    failure_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_terminal_commands_device FOREIGN KEY (device_id) REFERENCES payment_terminal_devices (id),
    CONSTRAINT fk_payment_terminal_commands_transaction FOREIGN KEY (terminal_transaction_id) REFERENCES payment_terminal_transactions (id),
    CONSTRAINT ck_payment_terminal_commands_type CHECK (command_type IN ('INITIATE_PAYMENT','QUERY_PAYMENT','CANCEL_PAYMENT')),
    CONSTRAINT ck_payment_terminal_commands_status CHECK (status IN ('QUEUED','DELIVERED','ACKNOWLEDGED','COMPLETED','FAILED','EXPIRED','CANCELLED')),
    CONSTRAINT ck_payment_terminal_commands_payload_object CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT ck_payment_terminal_commands_result_object CHECK (jsonb_typeof(result) = 'object'),
    CONSTRAINT ck_payment_terminal_commands_attempts CHECK (delivery_attempts >= 0),
    CONSTRAINT ck_payment_terminal_commands_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_payment_terminal_commands_terminal_state CHECK (
        (status IN ('COMPLETED','FAILED') AND completed_at IS NOT NULL) OR
        (status NOT IN ('COMPLETED','FAILED') AND completed_at IS NULL)
    ),
    CONSTRAINT uq_payment_terminal_commands_idempotency UNIQUE (idempotency_key)
);
CREATE UNIQUE INDEX uq_payment_terminal_commands_initiation
    ON payment_terminal_commands (terminal_transaction_id)
    WHERE command_type = 'INITIATE_PAYMENT';
CREATE INDEX idx_payment_terminal_commands_delivery
    ON payment_terminal_commands (device_id, status, available_at, created_at)
    WHERE status = 'QUEUED';
CREATE INDEX idx_payment_terminal_commands_expiry
    ON payment_terminal_commands (expires_at) WHERE status IN ('QUEUED','DELIVERED','ACKNOWLEDGED');

ALTER TABLE payment_terminal_transactions
    ADD COLUMN terminal_device_id UUID,
    ADD COLUMN bridge_command_id UUID,
    ADD COLUMN bridge_delivery_status VARCHAR(30),
    ADD CONSTRAINT fk_payment_terminal_transactions_device FOREIGN KEY (terminal_device_id) REFERENCES payment_terminal_devices (id),
    ADD CONSTRAINT fk_payment_terminal_transactions_command FOREIGN KEY (bridge_command_id) REFERENCES payment_terminal_commands (id),
    ADD CONSTRAINT ck_payment_terminal_transactions_bridge_status CHECK (
        bridge_delivery_status IS NULL OR bridge_delivery_status IN ('QUEUED','DELIVERED','ACKNOWLEDGED','COMPLETED','FAILED','EXPIRED','CANCELLED')
    );

CREATE INDEX idx_payment_terminal_transactions_device ON payment_terminal_transactions (terminal_device_id) WHERE terminal_device_id IS NOT NULL;
CREATE UNIQUE INDEX uq_payment_terminal_transactions_bridge_command ON payment_terminal_transactions (bridge_command_id) WHERE bridge_command_id IS NOT NULL;
