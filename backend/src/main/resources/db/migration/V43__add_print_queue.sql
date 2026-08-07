CREATE TABLE print_jobs (
    id UUID PRIMARY KEY,
    checkout_session_id UUID NOT NULL,
    requested_by_user_id BIGINT,
    status VARCHAR(16) NOT NULL,
    receipt_text TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    printing_started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_print_jobs_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_print_jobs_requested_by
        FOREIGN KEY (requested_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_print_jobs_status
        CHECK (status IN ('PENDING', 'PRINTING', 'PRINTED', 'FAILED')),
    CONSTRAINT ck_print_jobs_attempts
        CHECK (attempts >= 0),
    CONSTRAINT ck_print_jobs_completed_state
        CHECK (
            (status IN ('PRINTED', 'FAILED') AND completed_at IS NOT NULL)
            OR
            (status IN ('PENDING', 'PRINTING') AND completed_at IS NULL)
        )
);

CREATE INDEX idx_print_jobs_queue
    ON print_jobs (created_at, id)
    WHERE status = 'PENDING';

CREATE UNIQUE INDEX uq_print_jobs_active_checkout
    ON print_jobs (checkout_session_id)
    WHERE status IN ('PENDING', 'PRINTING');
