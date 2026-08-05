CREATE TABLE bar_dishes (
    id BIGSERIAL PRIMARY KEY,

    category_id BIGINT NOT NULL,

    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,

    price_cents BIGINT NOT NULL,
    detail VARCHAR(120) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_dishes_category
        FOREIGN KEY (category_id)
        REFERENCES bar_categories (id),

    CONSTRAINT fk_bar_dishes_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_bar_dishes_name_not_blank
        CHECK (
            LENGTH(BTRIM(name)) > 0
        ),

    CONSTRAINT ck_bar_dishes_normalized_name_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_name)) > 0
        ),

    CONSTRAINT ck_bar_dishes_price_positive
        CHECK (
            price_cents > 0
        ),

    CONSTRAINT ck_bar_dishes_detail_not_blank
        CHECK (
            LENGTH(BTRIM(detail)) > 0
        )
);

CREATE INDEX idx_bar_dishes_category_active
    ON bar_dishes (
        category_id,
        active,
        name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_dishes_active_name
    ON bar_dishes (
        active,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_dishes_normalized_name
    ON bar_dishes (normalized_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_dishes_created_at
    ON bar_dishes (created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bar_dishes_deleted_at
    ON bar_dishes (deleted_at);
