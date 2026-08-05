DROP INDEX IF EXISTS uq_checkout_sessions_operation_source;

CREATE UNIQUE INDEX uq_checkout_sessions_operation_source_active
    ON checkout_sessions (
        operation_type,
        source_id
    )
    WHERE source_id IS NOT NULL
      AND status NOT IN (
          'CANCELLED',
          'EXPIRED',
          'FINALIZED'
      );

CREATE TABLE lava_work_orders (
    id BIGSERIAL PRIMARY KEY,

    registered_client_id BIGINT,

    customer_name_snapshot VARCHAR(120) NOT NULL,
    normalized_customer_name_snapshot VARCHAR(120) NOT NULL,
    customer_phone_digits_snapshot VARCHAR(11),

    vehicle_name_snapshot VARCHAR(120),
    normalized_vehicle_name_snapshot VARCHAR(120),
    vehicle_plate_snapshot VARCHAR(7),

    vehicle_size VARCHAR(20) NOT NULL,
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

    cancelled_at TIMESTAMPTZ,
    cancelled_by_user_id BIGINT,
    cancellation_reason VARCHAR(255),

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lava_work_orders_client
        FOREIGN KEY (registered_client_id)
        REFERENCES lava_clients (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_lava_work_orders_checkout
        FOREIGN KEY (checkout_session_id)
        REFERENCES checkout_sessions (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_lava_work_orders_cancelled_by
        FOREIGN KEY (cancelled_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_lava_work_orders_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id),

    CONSTRAINT ck_lava_work_orders_customer_name
        CHECK (LENGTH(BTRIM(customer_name_snapshot)) > 0),

    CONSTRAINT ck_lava_work_orders_normalized_customer_name
        CHECK (LENGTH(BTRIM(normalized_customer_name_snapshot)) > 0),

    CONSTRAINT ck_lava_work_orders_phone
        CHECK (
            customer_phone_digits_snapshot IS NULL
            OR customer_phone_digits_snapshot ~ '^[0-9]{10,11}$'
        ),

    CONSTRAINT ck_lava_work_orders_vehicle_name
        CHECK (
            vehicle_name_snapshot IS NULL
            OR LENGTH(BTRIM(vehicle_name_snapshot)) > 0
        ),

    CONSTRAINT ck_lava_work_orders_normalized_vehicle_name
        CHECK (
            (
                vehicle_name_snapshot IS NULL
                AND normalized_vehicle_name_snapshot IS NULL
            )
            OR
            (
                vehicle_name_snapshot IS NOT NULL
                AND LENGTH(BTRIM(normalized_vehicle_name_snapshot)) > 0
            )
        ),

    CONSTRAINT ck_lava_work_orders_plate
        CHECK (
            vehicle_plate_snapshot IS NULL
            OR vehicle_plate_snapshot ~ '^[A-Z]{3}([0-9]{4}|[0-9][A-Z][0-9]{2})$'
        ),

    CONSTRAINT ck_lava_work_orders_vehicle_size
        CHECK (vehicle_size IN ('SMALL', 'MEDIUM')),

    CONSTRAINT ck_lava_work_orders_status
        CHECK (
            status IN (
                'OPEN',
                'PAYMENT_PENDING',
                'PAID',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_lava_work_orders_amounts
        CHECK (
            subtotal_cents >= 0
            AND discount_cents >= 0
            AND discount_cents <= subtotal_cents
            AND total_cents = subtotal_cents - discount_cents
        ),

    CONSTRAINT ck_lava_work_orders_create_fingerprint
        CHECK (create_request_fingerprint ~ '^[a-f0-9]{64}$'),

    CONSTRAINT ck_lava_work_orders_prepare_fingerprint
        CHECK (
            prepare_request_fingerprint IS NULL
            OR prepare_request_fingerprint ~ '^[a-f0-9]{64}$'
        ),

    CONSTRAINT ck_lava_work_orders_preparation
        CHECK (
            (
                status = 'PAYMENT_PENDING'
                AND checkout_session_id IS NOT NULL
                AND prepare_idempotency_key IS NOT NULL
                AND prepare_request_fingerprint IS NOT NULL
                AND prepared_at IS NOT NULL
            )
            OR status <> 'PAYMENT_PENDING'
        ),

    CONSTRAINT ck_lava_work_orders_cancelled_data
        CHECK (
            (
                status = 'CANCELLED'
                AND cancelled_at IS NOT NULL
                AND cancelled_by_user_id IS NOT NULL
                AND cancellation_reason IS NOT NULL
                AND LENGTH(BTRIM(cancellation_reason)) >= 3
            )
            OR
            (
                status <> 'CANCELLED'
                AND cancelled_at IS NULL
                AND cancelled_by_user_id IS NULL
                AND cancellation_reason IS NULL
            )
        )
);

CREATE TABLE lava_work_order_lines (
    id BIGSERIAL PRIMARY KEY,

    work_order_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,

    service_name_snapshot VARCHAR(120) NOT NULL,
    normalized_service_name_snapshot VARCHAR(120) NOT NULL,
    price_cents BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lava_work_order_lines_order
        FOREIGN KEY (work_order_id)
        REFERENCES lava_work_orders (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_lava_work_order_lines_service
        FOREIGN KEY (service_id)
        REFERENCES lava_services (id),

    CONSTRAINT ck_lava_work_order_lines_name
        CHECK (LENGTH(BTRIM(service_name_snapshot)) > 0),

    CONSTRAINT ck_lava_work_order_lines_normalized_name
        CHECK (LENGTH(BTRIM(normalized_service_name_snapshot)) > 0),

    CONSTRAINT ck_lava_work_order_lines_price
        CHECK (price_cents > 0)
);

CREATE UNIQUE INDEX uq_lava_work_order_lines_service
    ON lava_work_order_lines (
        work_order_id,
        service_id
    );

CREATE UNIQUE INDEX uq_lava_work_orders_create_idempotency_key
    ON lava_work_orders (create_idempotency_key);

CREATE UNIQUE INDEX uq_lava_work_orders_prepare_idempotency_key
    ON lava_work_orders (prepare_idempotency_key)
    WHERE prepare_idempotency_key IS NOT NULL;

CREATE INDEX idx_lava_work_orders_status_updated
    ON lava_work_orders (
        status,
        updated_at DESC
    );

CREATE INDEX idx_lava_work_orders_customer_search
    ON lava_work_orders (normalized_customer_name_snapshot);

CREATE INDEX idx_lava_work_orders_phone
    ON lava_work_orders (customer_phone_digits_snapshot)
    WHERE customer_phone_digits_snapshot IS NOT NULL;

CREATE INDEX idx_lava_work_orders_vehicle_search
    ON lava_work_orders (normalized_vehicle_name_snapshot)
    WHERE normalized_vehicle_name_snapshot IS NOT NULL;

CREATE INDEX idx_lava_work_orders_plate
    ON lava_work_orders (vehicle_plate_snapshot)
    WHERE vehicle_plate_snapshot IS NOT NULL;

CREATE INDEX idx_lava_work_orders_checkout
    ON lava_work_orders (checkout_session_id)
    WHERE checkout_session_id IS NOT NULL;

CREATE INDEX idx_lava_work_order_lines_order
    ON lava_work_order_lines (
        work_order_id,
        id
    );
