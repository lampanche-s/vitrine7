WITH due_stock AS (
    SELECT
        line.catalog_entry_id,
        SUM(line.quantity)::INTEGER AS quantity
    FROM bar_tabs tab
    JOIN bar_tab_lines line
        ON line.tab_id = tab.id
    JOIN catalog_entries entry
        ON entry.id = line.catalog_entry_id
    WHERE tab.status = 'PAYMENT_DUE'
      AND line.entry_type_snapshot = 'ITEM'
      AND entry.entry_type = 'ITEM'
      AND entry.stock_enabled = TRUE
      AND entry.stock_quantity IS NOT NULL
    GROUP BY line.catalog_entry_id
)
UPDATE catalog_entries entry
SET
    stock_quantity = entry.stock_quantity + due_stock.quantity,
    updated_at = CURRENT_TIMESTAMP,
    version = entry.version + 1
FROM due_stock
WHERE entry.id = due_stock.catalog_entry_id;

UPDATE bar_tabs
SET
    status = 'OPEN',
    payment_due_at = NULL,
    payment_due_by_user_id = NULL,
    vehicle_name_snapshot = NULL,
    vehicle_plate_snapshot = NULL,
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE status = 'PAYMENT_DUE';

DROP INDEX IF EXISTS idx_bar_tabs_payment_due_at;

ALTER TABLE bar_tabs
    DROP CONSTRAINT ck_bar_tabs_payment_due,
    DROP CONSTRAINT ck_bar_tabs_status,
    DROP CONSTRAINT ck_bar_tabs_checkout_state,
    DROP CONSTRAINT fk_bar_tabs_payment_due_by;

ALTER TABLE bar_tabs
    DROP COLUMN payment_due_at,
    DROP COLUMN payment_due_by_user_id;

ALTER TABLE bar_tabs
    ADD CONSTRAINT ck_bar_tabs_status
        CHECK (
            status IN (
                'OPEN',
                'PAYMENT_PENDING',
                'CLOSED',
                'CANCELLED'
            )
        );

ALTER TABLE bar_tabs
    ADD CONSTRAINT ck_bar_tabs_checkout_state
        CHECK (
            (
                status IN ('OPEN', 'CANCELLED')
                AND checkout_session_id IS NULL
            )
            OR
            (
                status IN ('PAYMENT_PENDING', 'CLOSED')
                AND checkout_session_id IS NOT NULL
            )
        );
