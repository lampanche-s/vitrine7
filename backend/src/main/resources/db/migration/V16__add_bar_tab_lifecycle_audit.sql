ALTER TABLE bar_tabs
    ADD COLUMN closed_at TIMESTAMPTZ,
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN cancelled_by_user_id BIGINT,
    ADD COLUMN cancellation_reason VARCHAR(255);

ALTER TABLE bar_tabs
    ADD CONSTRAINT fk_bar_tabs_cancelled_by
        FOREIGN KEY (cancelled_by_user_id)
        REFERENCES users (id);

UPDATE bar_tabs
SET closed_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE status = 'CLOSED'
  AND closed_at IS NULL;

UPDATE bar_tabs
SET
    cancelled_at = COALESCE(updated_at, CURRENT_TIMESTAMP),
    cancelled_by_user_id = created_by_user_id,
    cancellation_reason =
        'Cancelamento registrado antes da auditoria V16'
WHERE status = 'CANCELLED'
  AND cancelled_at IS NULL;

ALTER TABLE bar_tabs
    ADD CONSTRAINT ck_bar_tabs_lifecycle_audit
        CHECK (
            (
                status IN ('OPEN', 'PAYMENT_PENDING')
                AND closed_at IS NULL
                AND cancelled_at IS NULL
                AND cancelled_by_user_id IS NULL
                AND cancellation_reason IS NULL
            )
            OR
            (
                status = 'CLOSED'
                AND closed_at IS NOT NULL
                AND cancelled_at IS NULL
                AND cancelled_by_user_id IS NULL
                AND cancellation_reason IS NULL
            )
            OR
            (
                status = 'CANCELLED'
                AND closed_at IS NULL
                AND cancelled_at IS NOT NULL
                AND cancelled_by_user_id IS NOT NULL
                AND cancellation_reason IS NOT NULL
                AND LENGTH(BTRIM(cancellation_reason))
                    BETWEEN 3 AND 255
            )
        );

CREATE INDEX idx_bar_tabs_closed_at
    ON bar_tabs (closed_at DESC)
    WHERE status = 'CLOSED';

CREATE INDEX idx_bar_tabs_cancelled_at
    ON bar_tabs (cancelled_at DESC)
    WHERE status = 'CANCELLED';
