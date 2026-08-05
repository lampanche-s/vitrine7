CREATE TABLE system_settings (
    id SMALLINT PRIMARY KEY,

    company_name VARCHAR(120) NOT NULL,
    cnpj VARCHAR(18) NOT NULL DEFAULT '',
    phone VARCHAR(20) NOT NULL DEFAULT '',
    address VARCHAR(255) NOT NULL DEFAULT '',

    cash_close_mode VARCHAR(20) NOT NULL,

    direct_sale_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cancel_password_required BOOLEAN NOT NULL DEFAULT TRUE,
    discount_password_required BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_system_settings_singleton
        CHECK (id = 1),

    CONSTRAINT ck_system_settings_cash_close_mode
        CHECK (
            cash_close_mode IN (
                'SHIFT',
                'DAILY',
                'MANUAL'
            )
        )
);

CREATE TABLE system_modules (
    module_key VARCHAR(30) PRIMARY KEY,

    display_name VARCHAR(80) NOT NULL,
    display_order SMALLINT NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_system_modules_key
        CHECK (
            module_key IN (
                'LAVA',
                'BAR',
                'USERS'
            )
        )
);

CREATE TABLE user_preferences (
    user_id BIGINT PRIMARY KEY,

    admin_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_system_modules_active_order
    ON system_modules (active, display_order);

INSERT INTO system_settings (
    id,
    company_name,
    cnpj,
    phone,
    address,
    cash_close_mode,
    direct_sale_enabled,
    cancel_password_required,
    discount_password_required
) VALUES (
    1,
    'Prime Lava & Espeto Bar',
    '00.000.000/0001-00',
    '(71) 99999-0000',
    'Rua Principal, 100 - Salvador/BA',
    'SHIFT',
    TRUE,
    TRUE,
    TRUE
);

INSERT INTO system_modules (
    module_key,
    display_name,
    display_order,
    active
) VALUES
    ('LAVA', 'Lava Jato', 10, TRUE),
    ('BAR', 'Espeto Bar', 20, TRUE),
    ('USERS', 'Usuários', 30, TRUE);

INSERT INTO user_preferences (
    user_id,
    admin_mode_enabled
)
SELECT
    id,
    FALSE
FROM users
WHERE deleted_at IS NULL;
