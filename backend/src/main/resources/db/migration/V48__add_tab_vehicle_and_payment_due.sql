-- Veículo vinculado ao consumo e consumo encerrado aguardando recebimento.

ALTER TABLE bar_tabs
    ADD COLUMN vehicle_name_snapshot VARCHAR(120),
    ADD COLUMN vehicle_plate_snapshot VARCHAR(7),
    ADD COLUMN payment_due_at TIMESTAMPTZ,
    ADD COLUMN payment_due_by_user_id BIGINT;

ALTER TABLE bar_tabs
    ADD CONSTRAINT fk_bar_tabs_payment_due_by
        FOREIGN KEY (payment_due_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL;

ALTER TABLE bar_tabs
    DROP CONSTRAINT ck_bar_tabs_status,
    DROP CONSTRAINT ck_bar_tabs_checkout_state;

ALTER TABLE bar_tabs
    ADD CONSTRAINT ck_bar_tabs_status
        CHECK (status IN ('OPEN', 'PAYMENT_PENDING', 'PAYMENT_DUE', 'CLOSED', 'CANCELLED')),
    ADD CONSTRAINT ck_bar_tabs_checkout_state
        CHECK (
            (status IN ('OPEN', 'PAYMENT_DUE', 'CANCELLED') AND checkout_session_id IS NULL)
            OR (status IN ('PAYMENT_PENDING', 'CLOSED') AND checkout_session_id IS NOT NULL)
        ),
    ADD CONSTRAINT ck_bar_tabs_vehicle_snapshot
        CHECK (
            (vehicle_name_snapshot IS NULL AND vehicle_plate_snapshot IS NULL)
            OR (
                LENGTH(BTRIM(vehicle_name_snapshot)) BETWEEN 1 AND 120
                AND vehicle_plate_snapshot ~ '^[A-Z]{3}([0-9]{4}|[0-9][A-Z][0-9]{2})$'
            )
        ),
    ADD CONSTRAINT ck_bar_tabs_payment_due
        CHECK (
            (status = 'PAYMENT_DUE' AND payment_due_at IS NOT NULL)
            OR status <> 'PAYMENT_DUE'
        );

CREATE INDEX idx_bar_tabs_payment_due_at
    ON bar_tabs (payment_due_at DESC)
    WHERE status = 'PAYMENT_DUE';
