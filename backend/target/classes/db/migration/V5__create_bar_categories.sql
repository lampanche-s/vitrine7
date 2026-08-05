CREATE TABLE bar_categories (
    id BIGSERIAL PRIMARY KEY,

    scope VARCHAR(30) NOT NULL,

    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,

    system_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_categories_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_bar_categories_scope
        CHECK (
            scope IN (
                'DIRECT_SALE_ITEM',
                'MENU_DISH'
            )
        ),

    CONSTRAINT ck_bar_categories_name_not_blank
        CHECK (
            LENGTH(BTRIM(name)) > 0
        ),

    CONSTRAINT ck_bar_categories_normalized_name_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_name)) > 0
        ),

    CONSTRAINT ck_bar_categories_system_default
        CHECK (
            system_default = FALSE
            OR (
                normalized_name = 'sem categoria'
                AND active = TRUE
                AND deleted_at IS NULL
            )
        )
);

CREATE UNIQUE INDEX uq_bar_categories_scope_name_active
    ON bar_categories (
        scope,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_bar_categories_system_default_scope
    ON bar_categories (scope)
    WHERE system_default = TRUE
      AND deleted_at IS NULL;

CREATE INDEX idx_bar_categories_scope_active
    ON bar_categories (
        scope,
        active,
        name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_categories_deleted_at
    ON bar_categories (deleted_at);

INSERT INTO bar_categories (
    scope,
    name,
    normalized_name,
    system_default,
    active
) VALUES
    (
        'DIRECT_SALE_ITEM',
        'Sem categoria',
        'sem categoria',
        TRUE,
        TRUE
    ),
    (
        'MENU_DISH',
        'Sem categoria',
        'sem categoria',
        TRUE,
        TRUE
    ),

    (
        'DIRECT_SALE_ITEM',
        'Bebidas',
        'bebidas',
        FALSE,
        TRUE
    ),
    (
        'DIRECT_SALE_ITEM',
        'Refrigerantes',
        'refrigerantes',
        FALSE,
        TRUE
    ),
    (
        'DIRECT_SALE_ITEM',
        'Águas e sucos',
        'águas e sucos',
        FALSE,
        TRUE
    ),

    (
        'MENU_DISH',
        'Espetos',
        'espetos',
        FALSE,
        TRUE
    ),
    (
        'MENU_DISH',
        'Porções',
        'porções',
        FALSE,
        TRUE
    ),
    (
        'MENU_DISH',
        'Combos',
        'combos',
        FALSE,
        TRUE
    );
