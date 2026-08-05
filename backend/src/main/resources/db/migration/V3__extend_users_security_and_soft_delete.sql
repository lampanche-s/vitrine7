ALTER TABLE users
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE users
    ADD COLUMN deleted_by_user_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT fk_users_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS uq_users_username;

DROP INDEX IF EXISTS uq_users_email_not_null;

CREATE UNIQUE INDEX uq_users_username_active
    ON users (LOWER(username))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_users_email_active
    ON users (LOWER(email))
    WHERE email IS NOT NULL
      AND deleted_at IS NULL;

CREATE INDEX idx_users_deleted_at
    ON users (deleted_at);

CREATE INDEX idx_users_active_role_status
    ON users (role, status)
    WHERE deleted_at IS NULL;
