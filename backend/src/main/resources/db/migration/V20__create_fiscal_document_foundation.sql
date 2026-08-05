CREATE TABLE fiscal_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    checkout_session_id UUID NOT NULL,
    payment_id UUID NOT NULL,

    operation_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,

    document_model VARCHAR(20) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,

    customer_cpf_digits VARCHAR(11),

    subtotal_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,

    status_code VARCHAR(80),
    status_reason VARCHAR(500),
    external_document_id VARCHAR(120),
    access_key VARCHAR(44),

    authorized_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_fiscal_documents_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id),

    CONSTRAINT fk_fiscal_documents_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT uq_fiscal_documents_checkout
        UNIQUE (checkout_session_id),

    CONSTRAINT ck_fiscal_documents_operation_type
        CHECK (
            operation_type IN (
                'BAR_DIRECT_SALE',
                'BAR_COMMAND',
                'LAVA_WORK_ORDER'
            )
        ),

    CONSTRAINT ck_fiscal_documents_source
        CHECK (source_id > 0),

    CONSTRAINT ck_fiscal_documents_model
        CHECK (document_model = 'NFCE'),

    CONSTRAINT ck_fiscal_documents_type
        CHECK (
            document_type IN (
                'NFCE_WITHOUT_CPF',
                'NFCE_WITH_CPF'
            )
        ),

    CONSTRAINT ck_fiscal_documents_status
        CHECK (
            status IN (
                'PENDING_CONFIGURATION',
                'PENDING',
                'PROCESSING',
                'AUTHORIZED',
                'REJECTED',
                'CANCELLED',
                'ERROR'
            )
        ),

    CONSTRAINT ck_fiscal_documents_amounts
        CHECK (
            subtotal_cents >= 0
            AND discount_cents >= 0
            AND discount_cents <= subtotal_cents
            AND total_cents = subtotal_cents - discount_cents
        ),

    CONSTRAINT ck_fiscal_documents_cpf
        CHECK (
            (
                document_type = 'NFCE_WITH_CPF'
                AND customer_cpf_digits ~ '^[0-9]{11}$'
            )
            OR
            (
                document_type = 'NFCE_WITHOUT_CPF'
                AND customer_cpf_digits IS NULL
            )
        ),

    CONSTRAINT ck_fiscal_documents_authorized_data
        CHECK (
            (
                status = 'AUTHORIZED'
                AND access_key IS NOT NULL
                AND access_key ~ '^[0-9]{44}$'
                AND authorized_at IS NOT NULL
            )
            OR
            (
                status <> 'AUTHORIZED'
                AND authorized_at IS NULL
            )
        ),

    CONSTRAINT ck_fiscal_documents_cancelled_data
        CHECK (
            (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            OR
            (status <> 'CANCELLED' AND cancelled_at IS NULL)
        ),

    CONSTRAINT ck_fiscal_documents_initial_pending
        CHECK (
            status <> 'PENDING_CONFIGURATION'
            OR (
                access_key IS NULL
                AND authorized_at IS NULL
                AND cancelled_at IS NULL
                AND external_document_id IS NULL
            )
        )
);

CREATE INDEX idx_fiscal_documents_checkout
    ON fiscal_documents (checkout_session_id);

CREATE INDEX idx_fiscal_documents_status_created
    ON fiscal_documents (status, created_at DESC);

CREATE INDEX idx_fiscal_documents_source_operation
    ON fiscal_documents (operation_type, source_id);

CREATE TABLE fiscal_document_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    fiscal_document_id UUID NOT NULL,
    direction VARCHAR(20) NOT NULL,
    xml_content TEXT NOT NULL,
    status_code VARCHAR(80),
    reason VARCHAR(500),
    external_message_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fiscal_document_messages_document
        FOREIGN KEY (fiscal_document_id)
        REFERENCES fiscal_documents (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_fiscal_document_messages_direction
        CHECK (direction IN ('SENT', 'RECEIVED')),

    CONSTRAINT ck_fiscal_document_messages_xml
        CHECK (
            LENGTH(BTRIM(xml_content)) > 0
            AND OCTET_LENGTH(xml_content) <= 1048576
        )
);

CREATE INDEX idx_fiscal_document_messages_document_created
    ON fiscal_document_messages (fiscal_document_id, created_at ASC, id ASC);
