\set ON_ERROR_STOP on
\pset pager off

SELECT :'confirmation' = 'RESET_VITRINE7' AS confirmed \gset

\if :confirmed
BEGIN;

UPDATE payment_provider_profiles
SET
    created_by_user_id = NULL,
    updated_by_user_id = NULL;

TRUNCATE TABLE
    cash_closures,
    print_jobs,
    payment_terminal_commands,
    payment_terminal_pairing_codes,
    payment_terminal_transactions,
    payment_terminal_devices,
    payments,
    bar_tab_lines,
    bar_tabs,
    checkout_sessions,
    clients,
    catalog_entries,
    auth_sessions
RESTART IDENTITY CASCADE;

DELETE FROM users
WHERE id <> (
    SELECT id
    FROM users
    WHERE role = 'SUPER_ADMIN'
      AND status = 'ATIVO'
      AND deleted_at IS NULL
    ORDER BY created_at, id
    LIMIT 1
);

COMMIT;

SELECT
    (SELECT COUNT(*) FROM users) AS usuarios,
    (SELECT COUNT(*) FROM catalog_entries) AS catalogo,
    (SELECT COUNT(*) FROM clients) AS clientes,
    (SELECT COUNT(*) FROM bar_tabs) AS comandas,
    (SELECT COUNT(*) FROM payments) AS pagamentos,
    (SELECT COUNT(*) FROM cash_closures) AS fechamentos_caixa,
    (SELECT COUNT(*) FROM print_jobs) AS impressoes,
    (SELECT COUNT(*) FROM payment_provider_profiles) AS perfis_pagamento;
\else
\echo 'Limpeza cancelada. Execute com -v confirmation=RESET_VITRINE7.'
\quit
\endif
