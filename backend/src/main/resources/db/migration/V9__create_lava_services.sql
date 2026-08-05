CREATE TABLE lava_services (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,

    category VARCHAR(80) NOT NULL,
    normalized_category VARCHAR(80) NOT NULL,

    small_vehicle_price_cents BIGINT NOT NULL,
    medium_vehicle_price_cents BIGINT NOT NULL,

    duration_minutes INTEGER NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lava_services_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_lava_services_name_not_blank
        CHECK (
            LENGTH(BTRIM(name)) > 0
        ),

    CONSTRAINT ck_lava_services_normalized_name_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_name)) > 0
        ),

    CONSTRAINT ck_lava_services_category_not_blank
        CHECK (
            LENGTH(BTRIM(category)) > 0
        ),

    CONSTRAINT ck_lava_services_normalized_category_not_blank
        CHECK (
            LENGTH(BTRIM(normalized_category)) > 0
        ),

    CONSTRAINT ck_lava_services_small_price_positive
        CHECK (
            small_vehicle_price_cents > 0
        ),

    CONSTRAINT ck_lava_services_medium_price_positive
        CHECK (
            medium_vehicle_price_cents > 0
        ),

    CONSTRAINT ck_lava_services_duration
        CHECK (
            duration_minutes BETWEEN 1 AND 1440
        )
);

CREATE UNIQUE INDEX uq_lava_services_name_active
    ON lava_services (normalized_name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_category_active
    ON lava_services (
        normalized_category,
        active,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_active_name
    ON lava_services (
        active,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_duration
    ON lava_services (duration_minutes)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_small_price
    ON lava_services (small_vehicle_price_cents)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_medium_price
    ON lava_services (medium_vehicle_price_cents)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_created_at
    ON lava_services (created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_lava_services_deleted_at
    ON lava_services (deleted_at);
