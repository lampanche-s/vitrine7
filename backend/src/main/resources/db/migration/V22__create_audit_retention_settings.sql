CREATE TABLE audit_retention_settings (
    id SMALLINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    retention_days INTEGER NOT NULL,
    batch_size INTEGER NOT NULL,
    last_run_at TIMESTAMPTZ,
    last_cutoff_at TIMESTAMPTZ,
    last_deleted_count BIGINT,
    last_run_status VARCHAR(20) NOT NULL,
    last_error_message VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by_user_id BIGINT,
    version BIGINT NOT NULL,
    CONSTRAINT ck_audit_retention_singleton
        CHECK (id = 1),
    CONSTRAINT ck_audit_retention_days_range
        CHECK (retention_days BETWEEN 30 AND 3650),
    CONSTRAINT ck_audit_retention_batch_range
        CHECK (batch_size BETWEEN 100 AND 10000),
    CONSTRAINT ck_audit_retention_status
        CHECK (last_run_status IN (
            'NEVER_RUN',
            'SUCCESS',
            'FAILED',
            'SKIPPED'
        )),
    CONSTRAINT fk_audit_retention_updated_by_user
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
);

INSERT INTO audit_retention_settings (
    id,
    enabled,
    retention_days,
    batch_size,
    last_run_status,
    updated_at,
    version
)
VALUES (
    1,
    false,
    365,
    1000,
    'NEVER_RUN',
    NOW(),
    0
);
