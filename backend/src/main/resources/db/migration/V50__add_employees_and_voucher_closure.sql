CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_employees_deleted_by
        FOREIGN KEY (deleted_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_employees_name
        CHECK (LENGTH(BTRIM(name)) BETWEEN 1 AND 80),
    CONSTRAINT ck_employees_normalized_name
        CHECK (LENGTH(BTRIM(normalized_name)) BETWEEN 1 AND 80)
);

CREATE UNIQUE INDEX uq_employees_available_normalized_name
    ON employees (normalized_name)
    WHERE deleted_at IS NULL;

ALTER TABLE bar_tabs
    ADD COLUMN employee_id BIGINT,
    ADD COLUMN closure_type VARCHAR(20);

ALTER TABLE bar_tabs
    ADD CONSTRAINT fk_bar_tabs_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    ADD CONSTRAINT ck_bar_tabs_single_owner
        CHECK (NOT (client_id IS NOT NULL AND employee_id IS NOT NULL));

UPDATE bar_tabs
SET closure_type = 'PAYMENT'
WHERE status = 'CLOSED';

ALTER TABLE bar_tabs
    DROP CONSTRAINT ck_bar_tabs_checkout_state;

ALTER TABLE bar_tabs
    ADD CONSTRAINT ck_bar_tabs_closure_type
        CHECK (closure_type IS NULL OR closure_type IN ('PAYMENT', 'VOUCHER')),
    ADD CONSTRAINT ck_bar_tabs_checkout_state
        CHECK (
            (status = 'OPEN' AND checkout_session_id IS NULL AND closure_type IS NULL)
            OR (status = 'CANCELLED' AND checkout_session_id IS NULL AND closure_type IS NULL)
            OR (status = 'PAYMENT_PENDING' AND checkout_session_id IS NOT NULL AND closure_type IS NULL)
            OR (status = 'CLOSED' AND closure_type = 'PAYMENT' AND checkout_session_id IS NOT NULL)
            OR (
                status = 'CLOSED'
                AND closure_type = 'VOUCHER'
                AND checkout_session_id IS NULL
                AND employee_id IS NOT NULL
            )
        );

CREATE INDEX idx_bar_tabs_employee_closed
    ON bar_tabs (employee_id, closed_at DESC)
    WHERE employee_id IS NOT NULL;
