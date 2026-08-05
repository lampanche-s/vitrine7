CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    checkout_session_id UUID NOT NULL,

    idempotency_key UUID NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,

    method VARCHAR(30) NOT NULL,
    processing_mode VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,

    amount_cents BIGINT NOT NULL,

    cash_received_cents BIGINT,
    cash_change_cents BIGINT,
    cash_confirmed_by_user_id BIGINT,

    manual_reason VARCHAR(255),

    approved_at TIMESTAMPTZ,
    approved_by_user_id BIGINT,

    declined_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,
    cancelled_by_user_id BIGINT,
    cancel_reason VARCHAR(255),

    reversed_at TIMESTAMPTZ,
    reversed_by_user_id BIGINT,
    reversal_reason VARCHAR(255),

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_payments_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id),

    CONSTRAINT fk_payments_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id),

    CONSTRAINT fk_payments_approved_by
        FOREIGN KEY (approved_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_payments_cash_confirmed_by
        FOREIGN KEY (cash_confirmed_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_payments_cancelled_by
        FOREIGN KEY (cancelled_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_payments_reversed_by
        FOREIGN KEY (reversed_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_payments_fingerprint
        CHECK (request_fingerprint ~ '^[a-f0-9]{64}$'),

    CONSTRAINT ck_payments_method
        CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'CASH')),

    CONSTRAINT ck_payments_processing_mode
        CHECK (processing_mode IN ('CASH', 'MANUAL_FALLBACK', 'TERMINAL_SIMULATED', 'TERMINAL_REAL')),

    CONSTRAINT ck_payments_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'APPROVED',
            'DECLINED',
            'CANCELLED',
            'REVERSAL_PENDING',
            'REVERSED',
            'REFUND_REQUESTED',
            'REFUNDED'
        )),

    CONSTRAINT ck_payments_amount_positive
        CHECK (amount_cents > 0),

    CONSTRAINT ck_payments_method_mode
        CHECK (
            (method = 'CASH' AND processing_mode = 'CASH')
            OR
            (
                method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PIX')
                AND processing_mode IN ('MANUAL_FALLBACK', 'TERMINAL_SIMULATED', 'TERMINAL_REAL')
            )
        ),

    CONSTRAINT ck_payments_cash_fields
        CHECK (
            (
                method = 'CASH'
                AND cash_received_cents IS NOT NULL
                AND cash_change_cents IS NOT NULL
                AND cash_confirmed_by_user_id IS NOT NULL
                AND cash_received_cents >= amount_cents
                AND cash_change_cents = cash_received_cents - amount_cents
            )
            OR
            (
                method <> 'CASH'
                AND cash_received_cents IS NULL
                AND cash_change_cents IS NULL
                AND cash_confirmed_by_user_id IS NULL
            )
        ),

    CONSTRAINT ck_payments_manual_reason
        CHECK (
            (
                processing_mode = 'MANUAL_FALLBACK'
                AND manual_reason IS NOT NULL
                AND LENGTH(BTRIM(manual_reason)) BETWEEN 3 AND 255
            )
            OR
            (
                processing_mode <> 'MANUAL_FALLBACK'
                AND manual_reason IS NULL
            )
        ),

    CONSTRAINT ck_payments_approved_data
        CHECK (
            (
                status IN ('APPROVED', 'REVERSAL_PENDING', 'REVERSED', 'REFUND_REQUESTED', 'REFUNDED')
                AND approved_at IS NOT NULL
            )
            OR
            (
                status NOT IN ('APPROVED', 'REVERSAL_PENDING', 'REVERSED', 'REFUND_REQUESTED', 'REFUNDED')
                AND approved_at IS NULL
            )
        ),

    CONSTRAINT ck_payments_declined_data
        CHECK (
            (status = 'DECLINED' AND declined_at IS NOT NULL)
            OR
            (status <> 'DECLINED' AND declined_at IS NULL)
        ),

    CONSTRAINT ck_payments_cancelled_data
        CHECK (
            (
                status = 'CANCELLED'
                AND cancelled_at IS NOT NULL
                AND cancelled_by_user_id IS NOT NULL
                AND cancel_reason IS NOT NULL
                AND LENGTH(BTRIM(cancel_reason)) BETWEEN 3 AND 255
            )
            OR
            (
                status <> 'CANCELLED'
                AND cancelled_at IS NULL
                AND cancelled_by_user_id IS NULL
                AND cancel_reason IS NULL
            )
        ),

    CONSTRAINT ck_payments_reversed_data
        CHECK (
            (
                status = 'REVERSED'
                AND reversed_at IS NOT NULL
                AND reversed_by_user_id IS NOT NULL
                AND reversal_reason IS NOT NULL
                AND LENGTH(BTRIM(reversal_reason)) BETWEEN 3 AND 255
            )
            OR
            (
                status <> 'REVERSED'
                AND reversed_at IS NULL
                AND reversed_by_user_id IS NULL
                AND reversal_reason IS NULL
            )
        )
);

CREATE UNIQUE INDEX uq_payments_idempotency_key
    ON payments (idempotency_key);

CREATE UNIQUE INDEX uq_payments_settled_checkout
    ON payments (checkout_session_id)
    WHERE status IN (
        'APPROVED',
        'REVERSAL_PENDING',
        'REVERSED',
        'REFUND_REQUESTED',
        'REFUNDED'
    );

CREATE INDEX idx_payments_checkout_created
    ON payments (checkout_session_id, created_at);

CREATE INDEX idx_payments_status_created
    ON payments (status, created_at DESC);

CREATE INDEX idx_payments_method_created
    ON payments (method, created_at DESC);

CREATE INDEX idx_payments_processing_mode
    ON payments (processing_mode, created_at DESC);

CREATE INDEX idx_payments_created_by
    ON payments (created_by_user_id, created_at DESC);

CREATE INDEX idx_payments_approved_at
    ON payments (approved_at DESC)
    WHERE approved_at IS NOT NULL;
