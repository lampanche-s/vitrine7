CREATE TABLE checkout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    idempotency_key UUID NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,

    business_area VARCHAR(20) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,

    source_id BIGINT,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    subtotal_cents BIGINT NOT NULL DEFAULT 0,
    discount_cents BIGINT NOT NULL DEFAULT 0,
    total_cents BIGINT NOT NULL DEFAULT 0,

    document_type VARCHAR(30),
    cpf_digits VARCHAR(11),

    expires_at TIMESTAMPTZ NOT NULL,

    paid_at TIMESTAMPTZ,
    finalized_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,
    cancelled_by_user_id BIGINT,
    cancel_reason VARCHAR(255),

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_checkout_sessions_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id),

    CONSTRAINT fk_checkout_sessions_cancelled_by
        FOREIGN KEY (cancelled_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_checkout_sessions_fingerprint
        CHECK (
            request_fingerprint ~ '^[a-f0-9]{64}$'
        ),

    CONSTRAINT ck_checkout_sessions_business_area
        CHECK (
            business_area IN (
                'BAR',
                'LAVA'
            )
        ),

    CONSTRAINT ck_checkout_sessions_operation_type
        CHECK (
            operation_type IN (
                'BAR_DIRECT_SALE',
                'BAR_COMMAND',
                'LAVA_WORK_ORDER'
            )
        ),

    CONSTRAINT ck_checkout_sessions_area_operation
        CHECK (
            (
                business_area = 'BAR'
                AND operation_type IN (
                    'BAR_DIRECT_SALE',
                    'BAR_COMMAND'
                )
            )
            OR
            (
                business_area = 'LAVA'
                AND operation_type = 'LAVA_WORK_ORDER'
            )
        ),

    CONSTRAINT ck_checkout_sessions_status
        CHECK (
            status IN (
                'DRAFT',
                'READY_FOR_PAYMENT',
                'PAYMENT_PROCESSING',
                'PAYMENT_FAILED',
                'PAID',
                'FINALIZED',
                'CANCELLED',
                'EXPIRED'
            )
        ),

    CONSTRAINT ck_checkout_sessions_amounts
        CHECK (
            subtotal_cents >= 0
            AND discount_cents >= 0
            AND discount_cents <= subtotal_cents
            AND total_cents = subtotal_cents - discount_cents
        ),

    CONSTRAINT ck_checkout_sessions_document_type
        CHECK (
            document_type IS NULL
            OR document_type IN (
                'NFCE_WITHOUT_CPF',
                'NFCE_WITH_CPF',
                'GENERAL_RECEIPT'
            )
        ),

    CONSTRAINT ck_checkout_sessions_cpf
        CHECK (
            (
                document_type IS NULL
                AND cpf_digits IS NULL
            )
            OR
            (
                document_type = 'NFCE_WITH_CPF'
                AND cpf_digits ~ '^[0-9]{11}$'
            )
            OR
            (
                document_type IN (
                    'NFCE_WITHOUT_CPF',
                    'GENERAL_RECEIPT'
                )
                AND cpf_digits IS NULL
            )
        ),

    CONSTRAINT ck_checkout_sessions_expiration
        CHECK (
            expires_at > created_at
        ),

    CONSTRAINT ck_checkout_sessions_cancelled_data
        CHECK (
            (
                status = 'CANCELLED'
                AND cancelled_at IS NOT NULL
                AND cancelled_by_user_id IS NOT NULL
                AND cancel_reason IS NOT NULL
                AND LENGTH(BTRIM(cancel_reason)) >= 3
            )
            OR
            (
                status <> 'CANCELLED'
                AND cancelled_at IS NULL
                AND cancelled_by_user_id IS NULL
                AND cancel_reason IS NULL
            )
        ),

    CONSTRAINT ck_checkout_sessions_paid_at
        CHECK (
            paid_at IS NULL
            OR status IN (
                'PAID',
                'FINALIZED'
            )
        ),

    CONSTRAINT ck_checkout_sessions_finalized_at
        CHECK (
            finalized_at IS NULL
            OR status = 'FINALIZED'
        )
);

CREATE UNIQUE INDEX uq_checkout_sessions_idempotency_key
    ON checkout_sessions (idempotency_key);

CREATE UNIQUE INDEX uq_checkout_sessions_operation_source
    ON checkout_sessions (
        operation_type,
        source_id
    )
    WHERE source_id IS NOT NULL;

CREATE INDEX idx_checkout_sessions_status
    ON checkout_sessions (
        status,
        created_at DESC
    );

CREATE INDEX idx_checkout_sessions_expiration
    ON checkout_sessions (
        expires_at
    )
    WHERE status IN (
        'DRAFT',
        'READY_FOR_PAYMENT',
        'PAYMENT_FAILED'
    );

CREATE INDEX idx_checkout_sessions_created_by
    ON checkout_sessions (
        created_by_user_id,
        created_at DESC
    );

CREATE INDEX idx_checkout_sessions_business_area
    ON checkout_sessions (
        business_area,
        created_at DESC
    );
