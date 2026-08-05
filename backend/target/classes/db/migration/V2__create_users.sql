CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(160),

    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,

    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_users_username UNIQUE (username),

    CONSTRAINT ck_users_role CHECK (
        role IN ('ADMINISTRADOR', 'OPERADOR')
    ),

    CONSTRAINT ck_users_status CHECK (
        status IN ('ATIVO', 'BLOQUEADO')
    )
);

CREATE UNIQUE INDEX uq_users_email_not_null
    ON users (email)
    WHERE email IS NOT NULL;

CREATE INDEX idx_users_status
    ON users (status);

CREATE INDEX idx_users_role
    ON users (role);
