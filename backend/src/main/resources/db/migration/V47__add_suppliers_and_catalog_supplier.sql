CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    cnpj_digits VARCHAR(14),
    phone_digits VARCHAR(11),
    cep_digits VARCHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_suppliers_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_suppliers_name
        CHECK (LENGTH(BTRIM(name)) BETWEEN 1 AND 120),
    CONSTRAINT ck_suppliers_normalized_name
        CHECK (LENGTH(BTRIM(normalized_name)) BETWEEN 1 AND 120),
    CONSTRAINT ck_suppliers_cnpj_digits
        CHECK (cnpj_digits IS NULL OR cnpj_digits ~ '^[0-9]{14}$'),
    CONSTRAINT ck_suppliers_phone_digits
        CHECK (phone_digits IS NULL OR phone_digits ~ '^[0-9]{10,11}$'),
    CONSTRAINT ck_suppliers_cep_digits
        CHECK (cep_digits IS NULL OR cep_digits ~ '^[0-9]{8}$')
);

CREATE INDEX idx_suppliers_name_available
    ON suppliers (normalized_name)
    WHERE deleted_at IS NULL;

ALTER TABLE catalog_entries
    ADD COLUMN supplier_id BIGINT;

ALTER TABLE catalog_entries
    ADD CONSTRAINT fk_catalog_entries_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id)
        ON DELETE SET NULL;

CREATE INDEX idx_catalog_entries_supplier_id
    ON catalog_entries (supplier_id);

ALTER TABLE catalog_entries
    ADD CONSTRAINT ck_catalog_entries_service_without_supplier
        CHECK (entry_type <> 'SERVICE' OR supplier_id IS NULL);
