DROP INDEX IF EXISTS uq_bar_tab_line_dish;
DROP INDEX IF EXISTS uq_bar_tab_line_sale_item;

ALTER TABLE bar_tab_lines
    DROP CONSTRAINT IF EXISTS fk_bar_tab_lines_dish;

ALTER TABLE bar_tab_lines
    DROP CONSTRAINT IF EXISTS fk_bar_tab_lines_sale_item;

ALTER TABLE bar_tab_lines
    DROP CONSTRAINT IF EXISTS ck_bar_tab_lines_type;

ALTER TABLE bar_tab_lines
    DROP CONSTRAINT IF EXISTS ck_bar_tab_lines_source;

ALTER TABLE bar_tab_lines
    DROP CONSTRAINT IF EXISTS ck_bar_tab_lines_category;

ALTER TABLE bar_tab_lines
    DROP COLUMN line_type;

ALTER TABLE bar_tab_lines
    DROP COLUMN dish_id;

ALTER TABLE bar_tab_lines
    DROP COLUMN sale_item_id;

ALTER TABLE bar_tab_lines
    DROP COLUMN category_name_snapshot;
