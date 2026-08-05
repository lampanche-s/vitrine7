CREATE TABLE lava_clients (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,

    phone_digits VARCHAR(11),

    vehicle_name VARCHAR(120) NOT NULL,
    normalized_vehicle_name VARCHAR(120) NOT NULL,

    plate VARCHAR(7) NOT NULL,

    visits_count INTEGER NOT NULL DEFAULT 0,

    last_service_label VARCHAR(120),
    last_service_at TIMESTAMPTZ,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lava_clients_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_lava_clients_name_not_blank
        CHECK (
            LENGTH(BTRIM(name)) > 0
        ),

    CONSTRAINT ck_lava_clients_normalized_name_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_name)) > 0
        ),

    CONSTRAINT ck_lava_clients_vehicle_not_blank
        CHECK (
            LENGTH(BTRIM(vehicle_name)) > 0
        ),

    CONSTRAINT ck_lava_clients_normalized_vehicle_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_vehicle_name)) > 0
        ),

    CONSTRAINT ck_lava_clients_phone
        CHECK (
            phone_digits IS NULL
            OR phone_digits ~ '^[0-9]{10,11}$'
        ),

    CONSTRAINT ck_lava_clients_plate
        CHECK (
            plate ~ '^[A-Z]{3}([0-9]{4}|[0-9][A-Z][0-9]{2})$'
        ),

    CONSTRAINT ck_lava_clients_visits_non_negative
        CHECK (
            visits_count >= 0
        )
);

CREATE UNIQUE INDEX uq_lava_clients_plate_active
    ON lava_clients (plate)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_active_name
    ON lava_clients (
        active,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_normalized_name
    ON lava_clients (normalized_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_vehicle
    ON lava_clients (normalized_vehicle_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_phone
    ON lava_clients (phone_digits)
    WHERE deleted_at IS NULL
      AND phone_digits IS NOT NULL;

CREATE INDEX idx_lava_clients_visits
    ON lava_clients (visits_count DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_last_service
    ON lava_clients (last_service_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_created_at
    ON lava_clients (created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_clients_deleted_at
    ON lava_clients (deleted_at);
