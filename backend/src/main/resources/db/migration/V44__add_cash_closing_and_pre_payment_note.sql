ALTER TABLE print_jobs
    ADD COLUMN document_kind VARCHAR(24) NOT NULL DEFAULT 'RECEIPT';

ALTER TABLE print_jobs
    ADD CONSTRAINT ck_print_jobs_document_kind
        CHECK (document_kind IN ('RECEIPT', 'PREPAYMENT_NOTE'));

DROP INDEX IF EXISTS uq_print_jobs_active_checkout;

CREATE UNIQUE INDEX uq_print_jobs_active_document
    ON print_jobs (checkout_session_id, document_kind)
    WHERE status IN ('PENDING', 'PRINTING');

CREATE TABLE cash_closures (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    business_date DATE NOT NULL,
    total_received_cents BIGINT NOT NULL DEFAULT 0,
    sale_count BIGINT NOT NULL DEFAULT 0,
    reversed_cents BIGINT NOT NULL DEFAULT 0,
    reversed_count BIGINT NOT NULL DEFAULT 0,
    cash_cents BIGINT NOT NULL DEFAULT 0,
    pix_cents BIGINT NOT NULL DEFAULT 0,
    credit_cents BIGINT NOT NULL DEFAULT 0,
    debit_cents BIGINT NOT NULL DEFAULT 0,
    first_sale_at TIMESTAMPTZ,
    last_sale_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cash_closures_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_cash_closures_user_date
        UNIQUE (user_id, business_date),
    CONSTRAINT ck_cash_closures_non_negative
        CHECK (
            total_received_cents >= 0
            AND sale_count >= 0
            AND reversed_cents >= 0
            AND reversed_count >= 0
            AND cash_cents >= 0
            AND pix_cents >= 0
            AND credit_cents >= 0
            AND debit_cents >= 0
        )
);

CREATE INDEX idx_cash_closures_user_date
    ON cash_closures (user_id, business_date DESC);
