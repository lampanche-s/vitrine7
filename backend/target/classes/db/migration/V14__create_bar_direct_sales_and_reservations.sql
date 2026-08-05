CREATE TABLE bar_direct_sales (
    id BIGSERIAL PRIMARY KEY,

    checkout_session_id UUID NOT NULL,

    subtotal_cents BIGINT NOT NULL DEFAULT 0,
    discount_cents BIGINT NOT NULL DEFAULT 0,
    total_cents BIGINT NOT NULL DEFAULT 0,

    prepare_idempotency_key UUID,
    prepare_request_fingerprint CHAR(64),
    prepared_at TIMESTAMPTZ,

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_direct_sales_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id),

    CONSTRAINT fk_bar_direct_sales_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id),

    CONSTRAINT ck_bar_direct_sales_amounts
        CHECK (
            subtotal_cents >= 0
            AND discount_cents >= 0
            AND discount_cents <= subtotal_cents
            AND total_cents = subtotal_cents - discount_cents
        ),

    CONSTRAINT ck_bar_direct_sales_preparation
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
        )
);

CREATE UNIQUE INDEX uq_bar_direct_sales_checkout
    ON bar_direct_sales (checkout_session_id);

CREATE INDEX idx_bar_direct_sales_prepare_key
    ON bar_direct_sales (prepare_idempotency_key)
    WHERE prepare_idempotency_key IS NOT NULL;

CREATE INDEX idx_bar_direct_sales_created_at
    ON bar_direct_sales (created_at DESC);

CREATE TABLE bar_direct_sale_items (
    id BIGSERIAL PRIMARY KEY,

    direct_sale_id BIGINT NOT NULL,
    sale_item_id BIGINT NOT NULL,

    item_name_snapshot VARCHAR(120) NOT NULL,
    category_name_snapshot VARCHAR(80) NOT NULL,

    unit_price_cents BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    line_total_cents BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_direct_sale_items_sale
        FOREIGN KEY (direct_sale_id)
        REFERENCES bar_direct_sales (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bar_direct_sale_items_catalog_item
        FOREIGN KEY (sale_item_id)
        REFERENCES bar_sale_items (id),

    CONSTRAINT ck_bar_direct_sale_items_name
        CHECK (LENGTH(BTRIM(item_name_snapshot)) > 0),

    CONSTRAINT ck_bar_direct_sale_items_category
        CHECK (LENGTH(BTRIM(category_name_snapshot)) > 0),

    CONSTRAINT ck_bar_direct_sale_items_price
        CHECK (unit_price_cents > 0),

    CONSTRAINT ck_bar_direct_sale_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_bar_direct_sale_items_total
        CHECK (
            line_total_cents =
                unit_price_cents * quantity
        )
);

CREATE UNIQUE INDEX uq_bar_direct_sale_item
    ON bar_direct_sale_items (
        direct_sale_id,
        sale_item_id
    );

CREATE INDEX idx_bar_direct_sale_items_sale
    ON bar_direct_sale_items (
        direct_sale_id,
        id
    );

CREATE TABLE bar_stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    checkout_session_id UUID NOT NULL,
    direct_sale_id BIGINT NOT NULL,
    sale_item_id BIGINT NOT NULL,

    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    consumed_at TIMESTAMPTZ,

    released_at TIMESTAMPTZ,
    release_reason VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_bar_stock_reservations_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id),

    CONSTRAINT fk_bar_stock_reservations_sale
        FOREIGN KEY (direct_sale_id)
        REFERENCES bar_direct_sales (id),

    CONSTRAINT fk_bar_stock_reservations_item
        FOREIGN KEY (sale_item_id)
        REFERENCES bar_sale_items (id),

    CONSTRAINT ck_bar_stock_reservations_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_bar_stock_reservations_status
        CHECK (
            status IN (
                'ACTIVE',
                'CONSUMED',
                'RELEASED',
                'EXPIRED'
            )
        ),

    CONSTRAINT ck_bar_stock_reservations_expiration
        CHECK (expires_at > created_at),

    CONSTRAINT ck_bar_stock_reservations_lifecycle
        CHECK (
            (
                status = 'ACTIVE'
                AND consumed_at IS NULL
                AND released_at IS NULL
                AND release_reason IS NULL
            )
            OR
            (
                status = 'CONSUMED'
                AND consumed_at IS NOT NULL
                AND released_at IS NULL
                AND release_reason IS NULL
            )
            OR
            (
                status = 'RELEASED'
                AND consumed_at IS NULL
                AND released_at IS NOT NULL
                AND release_reason IS NOT NULL
                AND LENGTH(BTRIM(release_reason))
                    BETWEEN 3 AND 255
            )
            OR
            (
                status = 'EXPIRED'
                AND consumed_at IS NULL
                AND released_at IS NULL
                AND release_reason IS NULL
            )
        )
);

CREATE UNIQUE INDEX uq_bar_stock_reservation_checkout_item
    ON bar_stock_reservations (
        checkout_session_id,
        sale_item_id
    );

CREATE UNIQUE INDEX uq_bar_stock_reservation_sale_item
    ON bar_stock_reservations (
        direct_sale_id,
        sale_item_id
    );

CREATE INDEX idx_bar_stock_reservations_item_status
    ON bar_stock_reservations (
        sale_item_id,
        status,
        expires_at
    );

CREATE INDEX idx_bar_stock_reservations_checkout
    ON bar_stock_reservations (
        checkout_session_id,
        status
    );

CREATE INDEX idx_bar_stock_reservations_expiration
    ON bar_stock_reservations (expires_at)
    WHERE status = 'ACTIVE';
