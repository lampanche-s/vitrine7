ALTER TABLE catalog_entries
    ADD COLUMN stock_quantity INTEGER,
    ADD COLUMN minimum_stock_quantity INTEGER;

-- Itens existentes começam zerados para que o estoque real seja informado
-- antes de novas vendas. Serviços não controlam estoque.
UPDATE catalog_entries
SET
    stock_quantity = 0,
    minimum_stock_quantity = 0
WHERE entry_type = 'ITEM';

ALTER TABLE catalog_entries
    ADD CONSTRAINT ck_catalog_entries_stock_by_type
        CHECK (
            (
                entry_type = 'ITEM'
                AND stock_quantity IS NOT NULL
                AND minimum_stock_quantity IS NOT NULL
                AND stock_quantity >= 0
                AND minimum_stock_quantity >= 0
            )
            OR
            (
                entry_type = 'SERVICE'
                AND stock_quantity IS NULL
                AND minimum_stock_quantity IS NULL
            )
        );

CREATE INDEX idx_catalog_entries_stock_alert
    ON catalog_entries (
        stock_quantity,
        minimum_stock_quantity
    )
    WHERE entry_type = 'ITEM'
      AND deleted_at IS NULL;
