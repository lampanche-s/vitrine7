ALTER TABLE catalog_entries
    ADD COLUMN stock_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Preserva o comportamento dos itens que já estavam sob controle na V41.
UPDATE catalog_entries
SET stock_enabled = TRUE
WHERE entry_type = 'ITEM'
  AND stock_quantity IS NOT NULL
  AND minimum_stock_quantity IS NOT NULL;

ALTER TABLE catalog_entries
    DROP CONSTRAINT ck_catalog_entries_stock_by_type;

DROP INDEX idx_catalog_entries_stock_alert;

ALTER TABLE catalog_entries
    ADD CONSTRAINT ck_catalog_entries_stock_configuration
        CHECK (
            (
                entry_type = 'ITEM'
                AND (
                    (
                        stock_enabled = TRUE
                        AND stock_quantity IS NOT NULL
                        AND minimum_stock_quantity IS NOT NULL
                        AND stock_quantity >= 0
                        AND minimum_stock_quantity >= 0
                    )
                    OR
                    (
                        stock_enabled = FALSE
                        AND stock_quantity IS NULL
                        AND minimum_stock_quantity IS NULL
                    )
                )
            )
            OR
            (
                entry_type = 'SERVICE'
                AND stock_enabled = FALSE
                AND stock_quantity IS NULL
                AND minimum_stock_quantity IS NULL
            )
        );

CREATE INDEX idx_catalog_entries_stock_alert
    ON catalog_entries (
        stock_quantity,
        minimum_stock_quantity
    )
    WHERE deleted_at IS NULL
      AND entry_type = 'ITEM'
      AND stock_enabled = TRUE;
