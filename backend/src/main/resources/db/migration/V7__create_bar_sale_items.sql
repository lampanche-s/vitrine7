CREATE TABLE bar_sale_items (
    id BIGSERIAL PRIMARY KEY,

    category_id BIGINT NOT NULL,

    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,

    price_cents BIGINT NOT NULL,
    detail VARCHAR(120) NOT NULL DEFAULT '',

    stock_quantity INTEGER NOT NULL DEFAULT 0,
    minimum_stock_quantity INTEGER NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_sale_items_category
        FOREIGN KEY (category_id)
        REFERENCES bar_categories (id),

    CONSTRAINT fk_bar_sale_items_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_bar_sale_items_name_not_blank
        CHECK (
            LENGTH(BTRIM(name)) > 0
        ),

    CONSTRAINT ck_bar_sale_items_normalized_name_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_name)) > 0
        ),

    CONSTRAINT ck_bar_sale_items_price_positive
        CHECK (
            price_cents > 0
        ),

    CONSTRAINT ck_bar_sale_items_stock_non_negative
        CHECK (
            stock_quantity >= 0
        ),

    CONSTRAINT ck_bar_sale_items_minimum_stock_non_negative
        CHECK (
            minimum_stock_quantity >= 0
        )
);

CREATE INDEX idx_bar_sale_items_category_active
    ON bar_sale_items (
        category_id,
        active,
        name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_sale_items_active_name
    ON bar_sale_items (
        active,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_sale_items_normalized_name
    ON bar_sale_items (normalized_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_sale_items_stock
    ON bar_sale_items (
        stock_quantity,
        minimum_stock_quantity
    )
    WHERE deleted_at IS NULL
      AND active = TRUE;

CREATE INDEX idx_bar_sale_items_created_at
    ON bar_sale_items (created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_sale_items_deleted_at
    ON bar_sale_items (deleted_at);
