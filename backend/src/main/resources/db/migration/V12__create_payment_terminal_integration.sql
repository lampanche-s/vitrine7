CREATE TABLE payment_terminal_settings (
    id SMALLINT PRIMARY KEY,
    provider VARCHAR(30),
    mode VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    simulated_outcome VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_payment_terminal_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_payment_terminal_settings_provider CHECK (
        provider IS NULL OR provider IN ('STONE', 'PAGBANK', 'CIELO', 'TON', 'C6')
    ),
    CONSTRAINT ck_payment_terminal_settings_mode CHECK (mode IN ('SIMULATED', 'REAL')),
    CONSTRAINT ck_payment_terminal_settings_outcome CHECK (simulated_outcome IN ('APPROVED', 'DECLINED')),
    CONSTRAINT ck_payment_terminal_settings_active_provider CHECK (active = FALSE OR provider IS NOT NULL)
);

CREATE TABLE payment_terminal_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    checkout_session_id UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_cents BIGINT NOT NULL,
    provider_transaction_id VARCHAR(120),
    response_code VARCHAR(40),
    response_message VARCHAR(255),
    error_code VARCHAR(80),
    error_message VARCHAR(255),
    sent_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    declined_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_terminal_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_terminal_transactions_checkout FOREIGN KEY (checkout_session_id) REFERENCES checkout_sessions (id),
    CONSTRAINT fk_payment_terminal_transactions_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_payment_terminal_transactions_fingerprint CHECK (request_fingerprint ~ '^[a-f0-9]{64}$'),
    CONSTRAINT ck_payment_terminal_transactions_provider CHECK (provider IN ('STONE', 'PAGBANK', 'CIELO', 'TON', 'C6')),
    CONSTRAINT ck_payment_terminal_transactions_mode CHECK (mode IN ('SIMULATED', 'REAL')),
    CONSTRAINT ck_payment_terminal_transactions_method CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PIX')),
    CONSTRAINT ck_payment_terminal_transactions_status CHECK (status IN ('SENT', 'APPROVED', 'DECLINED', 'ERROR')),
    CONSTRAINT ck_payment_terminal_transactions_amount CHECK (amount_cents > 0),
    CONSTRAINT ck_payment_terminal_transactions_approved CHECK (
        (
            status = 'APPROVED'
            AND approved_at IS NOT NULL
            AND declined_at IS NULL
            AND failed_at IS NULL
            AND provider_transaction_id IS NOT NULL
        )
        OR (status <> 'APPROVED' AND approved_at IS NULL)
    ),
    CONSTRAINT ck_payment_terminal_transactions_declined CHECK (
        (
            status = 'DECLINED'
            AND declined_at IS NOT NULL
            AND approved_at IS NULL
            AND failed_at IS NULL
            AND provider_transaction_id IS NOT NULL
        )
        OR (status <> 'DECLINED' AND declined_at IS NULL)
    ),
    CONSTRAINT ck_payment_terminal_transactions_error CHECK (
        (
            status = 'ERROR'
            AND failed_at IS NOT NULL
            AND approved_at IS NULL
            AND declined_at IS NULL
            AND error_message IS NOT NULL
        )
        OR (
            status <> 'ERROR'
            AND failed_at IS NULL
            AND error_code IS NULL
            AND error_message IS NULL
        )
    )
);

CREATE UNIQUE INDEX uq_payment_terminal_transaction_payment
    ON payment_terminal_transactions (payment_id);

CREATE UNIQUE INDEX uq_payment_terminal_transaction_idempotency
    ON payment_terminal_transactions (idempotency_key);

CREATE INDEX idx_payment_terminal_transaction_checkout
    ON payment_terminal_transactions (checkout_session_id, created_at);

CREATE INDEX idx_payment_terminal_transaction_status
    ON payment_terminal_transactions (status, created_at DESC);

CREATE INDEX idx_payment_terminal_transaction_provider
    ON payment_terminal_transactions (provider, created_at DESC);

CREATE INDEX idx_payment_terminal_transaction_provider_id
    ON payment_terminal_transactions (provider, provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

INSERT INTO payment_terminal_settings (
    id,
    provider,
    mode,
    active,
    simulated_outcome
) VALUES (
    1,
    NULL,
    'SIMULATED',
    FALSE,
    'APPROVED'
);
