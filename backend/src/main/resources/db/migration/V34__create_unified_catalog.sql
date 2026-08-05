CREATE TABLE catalog_entries (
    id BIGSERIAL PRIMARY KEY,

    entry_type VARCHAR(20) NOT NULL,

    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,

    price_cents BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    deleted_by_user_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0,

    legacy_source VARCHAR(20) NOT NULL,
    legacy_id BIGINT NOT NULL,

    CONSTRAINT fk_catalog_entries_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_catalog_entries_type
        CHECK (
            entry_type IN (
                'ITEM',
                'SERVICE'
            )
        ),

    CONSTRAINT ck_catalog_entries_name
        CHECK (
            LENGTH(BTRIM(name)) BETWEEN 1 AND 120
        ),

    CONSTRAINT ck_catalog_entries_normalized_name
        CHECK (
            LENGTH(BTRIM(normalized_name)) BETWEEN 1 AND 120
        ),

    CONSTRAINT ck_catalog_entries_price
        CHECK (
            price_cents > 0
        ),

    CONSTRAINT ck_catalog_entries_legacy_source
        CHECK (
            legacy_source IN (
                'DISH',
                'SALE_ITEM'
            )
        ),

    CONSTRAINT uq_catalog_entries_legacy_source
        UNIQUE (
            legacy_source,
            legacy_id
        )
);

-- A migração deve parar explicitamente caso existam dois cadastros
-- disponíveis com o mesmo tipo e nome normalizado.
-- Isso evita escolher silenciosamente qual registro deve sobreviver.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT
                candidate.entry_type,
                candidate.normalized_name
            FROM (
                SELECT
                    CASE
                        WHEN LOWER(
                            BTRIM(category.normalized_name)
                        ) LIKE 'servi%'
                            THEN 'SERVICE'
                        ELSE 'ITEM'
                    END AS entry_type,

                    dish.normalized_name

                FROM bar_dishes dish

                JOIN bar_categories category
                    ON category.id = dish.category_id

                WHERE dish.active = TRUE
                  AND dish.deleted_at IS NULL

                UNION ALL

                SELECT
                    'ITEM' AS entry_type,
                    sale_item.normalized_name

                FROM bar_sale_items sale_item

                WHERE sale_item.active = TRUE
                  AND sale_item.deleted_at IS NULL
            ) candidate

            GROUP BY
                candidate.entry_type,
                candidate.normalized_name

            HAVING COUNT(*) > 1
        ) duplicate
    ) THEN
        RAISE EXCEPTION
            'Existem cadastros ativos duplicados por tipo e nome. Resolva-os antes de aplicar V34.';
    END IF;
END
$$;

-- Migra pratos para Item ou Serviço.
-- A categoria deixa de ser um conceito do novo domínio.
INSERT INTO catalog_entries (
    entry_type,
    name,
    normalized_name,
    price_cents,
    created_at,
    updated_at,
    deleted_at,
    deleted_by_user_id,
    version,
    legacy_source,
    legacy_id
)
SELECT
    CASE
        WHEN LOWER(
            BTRIM(category.normalized_name)
        ) LIKE 'servi%'
            THEN 'SERVICE'
        ELSE 'ITEM'
    END,

    dish.name,
    dish.normalized_name,
    dish.price_cents,
    dish.created_at,
    dish.updated_at,

    CASE
        WHEN dish.deleted_at IS NOT NULL
            THEN dish.deleted_at

        WHEN dish.active = FALSE
            THEN dish.updated_at

        ELSE NULL
    END,

    dish.deleted_by_user_id,
    dish.version,
    'DISH',
    dish.id

FROM bar_dishes dish

JOIN bar_categories category
    ON category.id = dish.category_id;

-- Também suporta bancos antigos que ainda tenham itens de venda.
-- Como estoque será removido, eles passam a ser Item comum.
INSERT INTO catalog_entries (
    entry_type,
    name,
    normalized_name,
    price_cents,
    created_at,
    updated_at,
    deleted_at,
    deleted_by_user_id,
    version,
    legacy_source,
    legacy_id
)
SELECT
    'ITEM',
    sale_item.name,
    sale_item.normalized_name,
    sale_item.price_cents,
    sale_item.created_at,
    sale_item.updated_at,

    CASE
        WHEN sale_item.deleted_at IS NOT NULL
            THEN sale_item.deleted_at

        WHEN sale_item.active = FALSE
            THEN sale_item.updated_at

        ELSE NULL
    END,

    sale_item.deleted_by_user_id,
    sale_item.version,
    'SALE_ITEM',
    sale_item.id

FROM bar_sale_items sale_item;

CREATE UNIQUE INDEX uq_catalog_entries_type_name_available
    ON catalog_entries (
        entry_type,
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalog_entries_type_name
    ON catalog_entries (
        entry_type,
        normalized_name
    );

CREATE INDEX idx_catalog_entries_name
    ON catalog_entries (
        normalized_name
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalog_entries_deleted_at
    ON catalog_entries (
        deleted_at
    );

-- Expande as linhas de comanda para o domínio novo.
ALTER TABLE bar_tab_lines
    ADD COLUMN catalog_entry_id BIGINT;

ALTER TABLE bar_tab_lines
    ADD COLUMN entry_type_snapshot VARCHAR(20);

UPDATE bar_tab_lines line
SET
    catalog_entry_id = entry.id,
    entry_type_snapshot = entry.entry_type
FROM catalog_entries entry
WHERE line.line_type = 'DISH'
  AND entry.legacy_source = 'DISH'
  AND entry.legacy_id = line.dish_id;

UPDATE bar_tab_lines line
SET
    catalog_entry_id = entry.id,
    entry_type_snapshot = entry.entry_type
FROM catalog_entries entry
WHERE line.line_type = 'SALE_ITEM'
  AND entry.legacy_source = 'SALE_ITEM'
  AND entry.legacy_id = line.sale_item_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM bar_tab_lines
        WHERE catalog_entry_id IS NULL
           OR entry_type_snapshot IS NULL
    ) THEN
        RAISE EXCEPTION
            'Existem linhas de comanda que não puderam ser associadas ao catálogo unificado.';
    END IF;
END
$$;

ALTER TABLE bar_tab_lines
    ALTER COLUMN catalog_entry_id SET NOT NULL;

ALTER TABLE bar_tab_lines
    ALTER COLUMN entry_type_snapshot SET NOT NULL;

ALTER TABLE bar_tab_lines
    ADD CONSTRAINT fk_bar_tab_lines_catalog_entry
        FOREIGN KEY (catalog_entry_id)
        REFERENCES catalog_entries (id);

ALTER TABLE bar_tab_lines
    ADD CONSTRAINT ck_bar_tab_lines_entry_type_snapshot
        CHECK (
            entry_type_snapshot IN (
                'ITEM',
                'SERVICE'
            )
        );

CREATE UNIQUE INDEX uq_bar_tab_line_catalog_entry
    ON bar_tab_lines (
        tab_id,
        catalog_entry_id
    );

CREATE INDEX idx_bar_tab_lines_catalog_entry
    ON bar_tab_lines (
        catalog_entry_id,
        tab_id
    );

-- O mapeamento temporário não faz parte do domínio final.
ALTER TABLE catalog_entries
    DROP CONSTRAINT uq_catalog_entries_legacy_source;

ALTER TABLE catalog_entries
    DROP CONSTRAINT ck_catalog_entries_legacy_source;

ALTER TABLE catalog_entries
    DROP COLUMN legacy_source;

ALTER TABLE catalog_entries
    DROP COLUMN legacy_id;
