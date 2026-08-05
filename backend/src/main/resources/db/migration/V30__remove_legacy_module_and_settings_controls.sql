DROP TABLE IF EXISTS system_modules;

ALTER TABLE system_settings
    DROP COLUMN IF EXISTS cash_close_mode,
    DROP COLUMN IF EXISTS direct_sale_enabled,
    DROP COLUMN IF EXISTS cancel_password_required,
    DROP COLUMN IF EXISTS discount_password_required;
