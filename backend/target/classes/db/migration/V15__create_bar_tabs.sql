CREATE TABLE bar_tabs (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,

    status VARCHAR(30) NOT NULL,

    checkout_session_id UUID,

    subtotal_cents BIGINT NOT NULL DEFAULT 0,
    discount_cents BIGINT NOT NULL DEFAULT 0,
    total_cents BIGINT NOT NULL DEFAULT 0,

    create_idempotency_key UUID NOT NULL,
    create_request_fingerprint CHAR(64) NOT NULL,

    prepare_idempotency_key UUID,
    prepare_request_fingerprint CHAR(64),
    prepared_at TIMESTAMPTZ,

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_tabs_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id),

    CONSTRAINT fk_bar_tabs_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id),

    CONSTRAINT ck_bar_tabs_name
        CHECK (LENGTH(BTRIM(name)) BETWEEN 1 AND 80),

    CONSTRAINT ck_bar_tabs_normalized_name
        CHECK (LENGTH(BTRIM(normalized_name)) BETWEEN 1 AND 80),

    CONSTRAINT ck_bar_tabs_status
        CHECK (status IN ('OPEN', 'PAYMENT_PENDING', 'CLOSED', 'CANCELLED')),

    CONSTRAINT ck_bar_tabs_amounts
        CHECK (
            subtotal_cents >= 0
            AND discount_cents >= 0
            AND discount_cents <= subtotal_cents
            AND total_cents = subtotal_cents - discount_cents
        ),

    CONSTRAINT ck_bar_tabs_create_fingerprint
        CHECK (create_request_fingerprint ~ '^[a-f0-9]{64}$'),

    CONSTRAINT ck_bar_tabs_preparation
        CHECK (
            (
                prepare_idempotency_key IS NULL
                AND prepare_request_fingerprint IS NULL
                AND prepared_at IS NULL
            )
            OR
            (
                prepare_idempotency_key IS NOT NULL
                AND prepare_request_fingerprint ~ '^[a-f0-9]{64}$'
                AND prepared_at IS NOT NULL
            )
        ),

    CONSTRAINT ck_bar_tabs_checkout_state
        CHECK (
            (
                status IN ('OPEN', 'CANCELLED')
                AND checkout_session_id IS NULL
            )
            OR
            (
                status IN ('PAYMENT_PENDING', 'CLOSED')
                AND checkout_session_id IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uq_bar_tabs_create_idempotency
    ON bar_tabs (create_idempotency_key);

CREATE UNIQUE INDEX uq_bar_tabs_checkout
    ON bar_tabs (checkout_session_id)
    WHERE checkout_session_id IS NOT NULL;

CREATE INDEX idx_bar_tabs_status_updated
    ON bar_tabs (status, updated_at DESC);

CREATE INDEX idx_bar_tabs_normalized_name
    ON bar_tabs (normalized_name);

CREATE TABLE bar_tab_lines (
    id BIGSERIAL PRIMARY KEY,

    tab_id BIGINT NOT NULL,

    line_type VARCHAR(30) NOT NULL,

    dish_id BIGINT,
    sale_item_id BIGINT,

    item_name_snapshot VARCHAR(120) NOT NULL,
    category_name_snapshot VARCHAR(80) NOT NULL,

    unit_price_cents BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    line_total_cents BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_tab_lines_tab
        FOREIGN KEY (tab_id)
        REFERENCES bar_tabs (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bar_tab_lines_dish
        FOREIGN KEY (dish_id)
        REFERENCES bar_dishes (id),

    CONSTRAINT fk_bar_tab_lines_sale_item
        FOREIGN KEY (sale_item_id)
        REFERENCES bar_sale_items (id),

    CONSTRAINT ck_bar_tab_lines_type
        CHECK (line_type IN ('DISH', 'SALE_ITEM')),

    CONSTRAINT ck_bar_tab_lines_source
        CHECK (
            (
                line_type = 'DISH'
                AND dish_id IS NOT NULL
                AND sale_item_id IS NULL
            )
            OR
            (
                line_type = 'SALE_ITEM'
                AND dish_id IS NULL
                AND sale_item_id IS NOT NULL
            )
        ),

    CONSTRAINT ck_bar_tab_lines_name
        CHECK (LENGTH(BTRIM(item_name_snapshot)) > 0),

    CONSTRAINT ck_bar_tab_lines_category
        CHECK (LENGTH(BTRIM(category_name_snapshot)) > 0),

    CONSTRAINT ck_bar_tab_lines_price
        CHECK (unit_price_cents > 0),

    CONSTRAINT ck_bar_tab_lines_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_bar_tab_lines_total
        CHECK (line_total_cents = unit_price_cents * quantity)
);

CREATE UNIQUE INDEX uq_bar_tab_line_dish
    ON bar_tab_lines (tab_id, dish_id)
    WHERE dish_id IS NOT NULL;

CREATE UNIQUE INDEX uq_bar_tab_line_sale_item
    ON bar_tab_lines (tab_id, sale_item_id)
    WHERE sale_item_id IS NOT NULL;

CREATE INDEX idx_bar_tab_lines_tab
    ON bar_tab_lines (tab_id, id);

ALTER TABLE bar_stock_reservations
    ADD COLUMN tab_id BIGINT;

ALTER TABLE bar_stock_reservations
    ALTER COLUMN direct_sale_id DROP NOT NULL;

ALTER TABLE bar_stock_reservations
    ADD CONSTRAINT fk_bar_stock_reservations_tab
        FOREIGN KEY (tab_id)
        REFERENCES bar_tabs (id);

ALTER TABLE bar_stock_reservations
    ADD CONSTRAINT ck_bar_stock_reservations_origin
        CHECK (
            (
                direct_sale_id IS NOT NULL
                AND tab_id IS NULL
            )
            OR
            (
                direct_sale_id IS NULL
                AND tab_id IS NOT NULL
            )
        );

DROP INDEX uq_bar_stock_reservation_sale_item;

CREATE UNIQUE INDEX uq_bar_stock_reservation_direct_sale_item
    ON bar_stock_reservations (direct_sale_id, sale_item_id)
    WHERE direct_sale_id IS NOT NULL;

CREATE UNIQUE INDEX uq_bar_stock_reservation_tab_item
    ON bar_stock_reservations (tab_id, sale_item_id)
    WHERE tab_id IS NOT NULL;

CREATE INDEX idx_bar_stock_reservations_tab
    ON bar_stock_reservations (tab_id, status)
    WHERE tab_id IS NOT NULL;
