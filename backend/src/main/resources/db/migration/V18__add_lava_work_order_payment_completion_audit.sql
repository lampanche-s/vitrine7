ALTER TABLE lava_work_orders
    ADD COLUMN paid_at TIMESTAMPTZ,
    ADD COLUMN paid_by_user_id BIGINT,
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN completed_by_user_id BIGINT;

ALTER TABLE lava_work_orders
    ADD CONSTRAINT fk_lava_work_orders_paid_by
        FOREIGN KEY (paid_by_user_id)
        REFERENCES users (id),

    ADD CONSTRAINT fk_lava_work_orders_completed_by
        FOREIGN KEY (completed_by_user_id)
        REFERENCES users (id),

    ADD CONSTRAINT ck_lava_work_orders_payment_completion_audit
        CHECK (
            (
                status IN ('OPEN', 'PAYMENT_PENDING', 'CANCELLED')
                AND paid_at IS NULL
                AND paid_by_user_id IS NULL
                AND completed_at IS NULL
                AND completed_by_user_id IS NULL
            )
            OR
            (
                status = 'PAID'
                AND paid_at IS NOT NULL
                AND paid_by_user_id IS NOT NULL
                AND completed_at IS NULL
                AND completed_by_user_id IS NULL
            )
            OR
            (
                status = 'COMPLETED'
                AND paid_at IS NOT NULL
                AND paid_by_user_id IS NOT NULL
                AND completed_at IS NOT NULL
                AND completed_by_user_id IS NOT NULL
                AND completed_at >= paid_at
            )
        );

CREATE INDEX idx_lava_work_orders_paid_at
    ON lava_work_orders (paid_at DESC)
    WHERE paid_at IS NOT NULL;

CREATE INDEX idx_lava_work_orders_completed_at
    ON lava_work_orders (completed_at DESC)
    WHERE completed_at IS NOT NULL;
