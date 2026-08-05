CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    request_id UUID,
    actor_user_id BIGINT,
    actor_username_snapshot VARCHAR(120),
    actor_role_snapshot VARCHAR(40),
    module VARCHAR(60) NOT NULL,
    action VARCHAR(120) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(120),
    idempotency_key UUID,
    http_method VARCHAR(12),
    request_path VARCHAR(500),
    error_code VARCHAR(80),
    error_message VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_audit_logs_module_not_blank
        CHECK (BTRIM(module) <> ''),
    CONSTRAINT ck_audit_logs_action_not_blank
        CHECK (BTRIM(action) <> ''),
    CONSTRAINT ck_audit_logs_resource_type_not_blank
        CHECK (BTRIM(resource_type) <> ''),
    CONSTRAINT ck_audit_logs_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_audit_logs_metadata_object
        CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT fk_audit_logs_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_logs_occurred_at
    ON audit_logs (occurred_at DESC);

CREATE INDEX idx_audit_logs_module_occurred_at
    ON audit_logs (module, occurred_at DESC);

CREATE INDEX idx_audit_logs_action_occurred_at
    ON audit_logs (action, occurred_at DESC);

CREATE INDEX idx_audit_logs_actor_occurred_at
    ON audit_logs (actor_user_id, occurred_at DESC);

CREATE INDEX idx_audit_logs_resource
    ON audit_logs (resource_type, resource_id);

CREATE INDEX idx_audit_logs_request_id
    ON audit_logs (request_id);

CREATE INDEX idx_audit_logs_outcome
    ON audit_logs (outcome);
