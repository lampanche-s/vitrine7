CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,

    user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_auth_sessions_user_active
    ON auth_sessions (user_id, revoked_at, last_activity_at);

CREATE INDEX idx_auth_sessions_expires
    ON auth_sessions (expires_at)
    WHERE revoked_at IS NULL;
