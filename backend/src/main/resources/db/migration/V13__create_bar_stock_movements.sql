CREATE TABLE bar_stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    sale_item_id BIGINT NOT NULL,

    idempotency_key UUID,
    request_fingerprint CHAR(64),

    movement_type VARCHAR(30) NOT NULL,

    quantity_delta INTEGER NOT NULL,
    previous_quantity INTEGER NOT NULL,
    resulting_quantity INTEGER NOT NULL,

    reason VARCHAR(255) NOT NULL,

    checkout_session_id UUID,

    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bar_stock_movements_item
        FOREIGN KEY (sale_item_id)
        REFERENCES bar_sale_items (id),

    CONSTRAINT fk_bar_stock_movements_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id),

    CONSTRAINT fk_bar_stock_movements_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id),

    CONSTRAINT ck_bar_stock_movements_type
        CHECK (
            movement_type IN (
                'INITIAL_BALANCE',
                'ENTRY',
                'EXIT',
                'ADJUSTMENT',
                'SALE',
                'SALE_REVERSAL'
            )
        ),

    CONSTRAINT ck_bar_stock_movements_quantities
        CHECK (
            previous_quantity >= 0
            AND resulting_quantity >= 0
            AND quantity_delta <> 0
            AND resulting_quantity =
                previous_quantity + quantity_delta
        ),

    CONSTRAINT ck_bar_stock_movements_type_delta
        CHECK (
            (
                movement_type = 'INITIAL_BALANCE'
                AND quantity_delta > 0
                AND previous_quantity = 0
                AND resulting_quantity = quantity_delta
            )
            OR
            (
                movement_type = 'ENTRY'
                AND quantity_delta > 0
            )
            OR
            (
                movement_type = 'EXIT'
                AND quantity_delta < 0
            )
            OR
            (
                movement_type = 'ADJUSTMENT'
                AND quantity_delta <> 0
            )
            OR
            (
                movement_type = 'SALE'
                AND quantity_delta < 0
            )
            OR
            (
                movement_type = 'SALE_REVERSAL'
                AND quantity_delta > 0
            )
        ),

    CONSTRAINT ck_bar_stock_movements_reason
        CHECK (
            LENGTH(BTRIM(reason)) BETWEEN 3 AND 255
        ),

    CONSTRAINT ck_bar_stock_movements_fingerprint
        CHECK (
            request_fingerprint IS NULL
            OR request_fingerprint ~ '^[a-f0-9]{64}$'
        ),

    CONSTRAINT ck_bar_stock_movements_origin
        CHECK (
            (
                movement_type IN (
                    'ENTRY',
                    'EXIT',
                    'ADJUSTMENT'
                )
                AND idempotency_key IS NOT NULL
                AND request_fingerprint IS NOT NULL
                AND checkout_session_id IS NULL
            )
            OR
            (
                movement_type = 'INITIAL_BALANCE'
                AND idempotency_key IS NULL
                AND request_fingerprint IS NULL
                AND checkout_session_id IS NULL
            )
            OR
            (
                movement_type IN (
                    'SALE',
                    'SALE_REVERSAL'
                )
                AND idempotency_key IS NULL
                AND request_fingerprint IS NULL
                AND checkout_session_id IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uq_bar_stock_movements_idempotency
    ON bar_stock_movements (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uq_bar_stock_movements_checkout_type
    ON bar_stock_movements (
        sale_item_id,
        movement_type,
        checkout_session_id
    )
    WHERE checkout_session_id IS NOT NULL;

CREATE INDEX idx_bar_stock_movements_item_created
    ON bar_stock_movements (
        sale_item_id,
        created_at DESC
    );

CREATE INDEX idx_bar_stock_movements_type_created
    ON bar_stock_movements (
        movement_type,
        created_at DESC
    );

CREATE INDEX idx_bar_stock_movements_created_by
    ON bar_stock_movements (
        created_by_user_id,
        created_at DESC
    );

CREATE INDEX idx_bar_stock_movements_checkout
    ON bar_stock_movements (
        checkout_session_id
    )
    WHERE checkout_session_id IS NOT NULL;

INSERT INTO bar_stock_movements (
    sale_item_id,
    idempotency_key,
    request_fingerprint,
    movement_type,
    quantity_delta,
    previous_quantity,
    resulting_quantity,
    reason,
    checkout_session_id,
    created_by_user_id
)
SELECT
    item.id,
    NULL,
    NULL,
    'INITIAL_BALANCE',
    item.stock_quantity,
    0,
    item.stock_quantity,
    'Saldo inicial migrado na implantacao do controle de estoque',
    NULL,
    seed_user.id
FROM bar_sale_items item
CROSS JOIN LATERAL (
    SELECT users.id
    FROM users
    ORDER BY users.id
    LIMIT 1
) seed_user
WHERE item.stock_quantity > 0;
