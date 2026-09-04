CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    phone_digits VARCHAR(11),

    vehicle_name VARCHAR(120) NOT NULL,
    normalized_vehicle_name VARCHAR(120) NOT NULL,
    plate VARCHAR(7) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_clients_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_clients_name_not_blank
        CHECK (LENGTH(BTRIM(name)) > 0),

    CONSTRAINT ck_clients_normalized_name_not_blank
        CHECK (LENGTH(BTRIM(normalized_name)) > 0),

    CONSTRAINT ck_clients_vehicle_not_blank
        CHECK (LENGTH(BTRIM(vehicle_name)) > 0),

    CONSTRAINT ck_clients_normalized_vehicle_not_blank
        CHECK (LENGTH(BTRIM(normalized_vehicle_name)) > 0),

    CONSTRAINT ck_clients_phone
        CHECK (
            phone_digits IS NULL
            OR phone_digits ~ '^[0-9]{10,11}$'
        ),

    CONSTRAINT ck_clients_plate
        CHECK (
            plate ~ '^[A-Z]{3}([0-9]{4}|[0-9][A-Z][0-9]{2})$'
        )
);

INSERT INTO clients (
    id,
    name,
    normalized_name,
    phone_digits,
    vehicle_name,
    normalized_vehicle_name,
    plate,
    active,
    created_at,
    updated_at,
    deleted_at,
    deleted_by_user_id,
    version
)
SELECT
    id,
    name,
    normalized_name,
    phone_digits,
    vehicle_name,
    normalized_vehicle_name,
    plate,
    active,
    created_at,
    updated_at,
    deleted_at,
    deleted_by_user_id,
    version
FROM lava_clients;

SELECT setval(
    pg_get_serial_sequence('clients', 'id'),
    COALESCE((SELECT MAX(id) FROM clients), 1),
    EXISTS (SELECT 1 FROM clients)
);

CREATE UNIQUE INDEX uq_clients_plate_available
    ON clients (plate)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_clients_active_name
    ON clients (active, normalized_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_clients_search
    ON clients (normalized_name, normalized_vehicle_name, plate)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_clients_phone
    ON clients (phone_digits)
    WHERE deleted_at IS NULL
      AND phone_digits IS NOT NULL;

CREATE INDEX idx_clients_deleted_at
    ON clients (deleted_at);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM lava_services
    ) THEN
        RAISE EXCEPTION
            'Existem serviços no módulo Lava Jato. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM lava_work_orders
    ) THEN
        RAISE EXCEPTION
            'Existem ordens de serviço Lava Jato. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM lava_work_order_lines
    ) THEN
        RAISE EXCEPTION
            'Existem linhas de ordens Lava Jato. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM checkout_sessions
        WHERE operation_type = 'LAVA_WORK_ORDER'
           OR business_area = 'LAVA'
    ) THEN
        RAISE EXCEPTION
            'Existem checkouts do módulo Lava Jato. A remoção foi interrompida.';
    END IF;
END
$$;

DROP TABLE lava_work_order_lines;
DROP TABLE lava_work_orders;
DROP TABLE lava_services;
DROP TABLE lava_clients;

ALTER TABLE checkout_sessions
    DROP CONSTRAINT IF EXISTS ck_checkout_sessions_business_area;

ALTER TABLE checkout_sessions
    DROP CONSTRAINT IF EXISTS ck_checkout_sessions_operation_type;

ALTER TABLE checkout_sessions
    DROP CONSTRAINT IF EXISTS ck_checkout_sessions_area_operation;

ALTER TABLE checkout_sessions
    ADD CONSTRAINT ck_checkout_sessions_business_area
        CHECK (business_area = 'BAR');

ALTER TABLE checkout_sessions
    ADD CONSTRAINT ck_checkout_sessions_operation_type
        CHECK (operation_type = 'BAR_COMMAND');

ALTER TABLE checkout_sessions
    ADD CONSTRAINT ck_checkout_sessions_area_operation
        CHECK (
            business_area = 'BAR'
            AND operation_type = 'BAR_COMMAND'
        );
