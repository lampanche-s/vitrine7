ALTER TABLE bar_tab_lines
    DROP CONSTRAINT ck_bar_tab_lines_type;

ALTER TABLE bar_tab_lines
    DROP CONSTRAINT ck_bar_tab_lines_source;

UPDATE bar_tab_lines
SET line_type = 'CATALOG_ENTRY';

ALTER TABLE bar_tab_lines
    ADD CONSTRAINT ck_bar_tab_lines_type
        CHECK (
            line_type = 'CATALOG_ENTRY'
        );

ALTER TABLE bar_tab_lines
    ADD CONSTRAINT ck_bar_tab_lines_source
        CHECK (
            catalog_entry_id IS NOT NULL
        );
