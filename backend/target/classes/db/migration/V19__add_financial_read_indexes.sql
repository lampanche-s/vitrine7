CREATE INDEX idx_payments_status_approved_at
    ON payments (status, approved_at DESC)
    WHERE approved_at IS NOT NULL;

CREATE INDEX idx_payments_method_approved_at
    ON payments (method, approved_at DESC)
    WHERE approved_at IS NOT NULL;

CREATE INDEX idx_checkout_sessions_operation_type_id
    ON checkout_sessions (operation_type, id);
