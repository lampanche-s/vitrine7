-- V46: retomada de comanda, vínculo com cliente e pagamentos divididos.

ALTER TABLE bar_tabs
    ADD COLUMN client_id BIGINT,
    ADD COLUMN reopened_at TIMESTAMPTZ,
    ADD COLUMN reopened_by_user_id BIGINT,
    ADD COLUMN reopen_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE bar_tabs
    ADD CONSTRAINT fk_bar_tabs_client
        FOREIGN KEY (client_id)
        REFERENCES clients (id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_bar_tabs_reopened_by
        FOREIGN KEY (reopened_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,
    ADD CONSTRAINT ck_bar_tabs_reopen_count
        CHECK (reopen_count >= 0);

CREATE INDEX idx_bar_tabs_client_closed
    ON bar_tabs (client_id, closed_at DESC)
    WHERE client_id IS NOT NULL;

ALTER TABLE payments
    ADD COLUMN superseded_at TIMESTAMPTZ,
    ADD COLUMN superseded_by_user_id BIGINT,
    ADD COLUMN supersede_reason VARCHAR(255);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_superseded_by
        FOREIGN KEY (superseded_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL;

ALTER TABLE payments
    DROP CONSTRAINT ck_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'APPROVED',
                'DECLINED',
                'CANCELLED',
                'REVERSED',
                'SUPERSEDED'
            )
        );

ALTER TABLE payments
    DROP CONSTRAINT ck_payments_approved_data;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_approved_data
        CHECK (
            (
                status IN ('APPROVED', 'REVERSED', 'SUPERSEDED')
                AND approved_at IS NOT NULL
            )
            OR
            (
                status NOT IN ('APPROVED', 'REVERSED', 'SUPERSEDED')
                AND approved_at IS NULL
            )
        );

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_superseded_data
        CHECK (
            (
                status = 'SUPERSEDED'
                AND superseded_at IS NOT NULL
                AND superseded_by_user_id IS NOT NULL
                AND supersede_reason IS NOT NULL
                AND LENGTH(BTRIM(supersede_reason)) BETWEEN 3 AND 255
            )
            OR
            (
                status <> 'SUPERSEDED'
                AND superseded_at IS NULL
                AND superseded_by_user_id IS NULL
                AND supersede_reason IS NULL
            )
        );

DROP INDEX IF EXISTS uq_payments_settled_checkout;

CREATE INDEX idx_payments_settled_checkout
    ON payments (checkout_session_id, approved_at, created_at)
    WHERE status IN ('APPROVED', 'REVERSED');

CREATE INDEX idx_payments_checkout_status
    ON payments (checkout_session_id, status, created_at);
