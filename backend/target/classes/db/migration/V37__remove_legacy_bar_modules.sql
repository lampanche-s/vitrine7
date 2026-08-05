-- Impede perda silenciosa de dados que não fazem parte da
-- estratégia aprovada de migração.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM flyway_schema_history
        WHERE version = '34'
          AND success = TRUE
    ) THEN
        RAISE EXCEPTION
            'A migration do catálogo unificado V34 não está aplicada.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM bar_tab_lines
        WHERE catalog_entry_id IS NULL
           OR entry_type_snapshot IS NULL
    ) THEN
        RAISE EXCEPTION
            'Existem linhas de comanda sem vínculo com o catálogo unificado.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM checkout_sessions
        WHERE operation_type = 'BAR_DIRECT_SALE'
    ) THEN
        RAISE EXCEPTION
            'Existem checkouts de Venda Direta. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM bar_direct_sales
    ) THEN
        RAISE EXCEPTION
            'Existem vendas diretas persistidas. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM bar_direct_sale_items
    ) THEN
        RAISE EXCEPTION
            'Existem linhas de Venda Direta persistidas. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM bar_stock_movements
    ) THEN
        RAISE EXCEPTION
            'Existem movimentos de estoque persistidos. A remoção foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM bar_stock_reservations
    ) THEN
        RAISE EXCEPTION
            'Existem reservas de estoque persistidas. A remoção foi interrompida.';
    END IF;
END
$$;

-- Remove primeiro as tabelas dependentes.
DROP TABLE bar_stock_reservations;
DROP TABLE bar_stock_movements;
DROP TABLE bar_direct_sale_items;
DROP TABLE bar_direct_sales;

-- O catálogo unificado já substituiu estas estruturas.
DROP TABLE bar_sale_items;
DROP TABLE bar_dishes;
DROP TABLE bar_categories;

-- Descobre e remove somente o CHECK relacionado a operation_type,
-- independentemente do nome usado pela migration antiga.
DO $$
DECLARE
    operation_constraint RECORD;
BEGIN
    FOR operation_constraint IN
        SELECT constraint_data.conname
        FROM pg_constraint constraint_data
        WHERE constraint_data.conrelid =
              'checkout_sessions'::regclass
          AND constraint_data.contype = 'c'
          AND pg_get_constraintdef(
                  constraint_data.oid
              ) ILIKE '%operation_type%'
    LOOP
        EXECUTE format(
            'ALTER TABLE checkout_sessions DROP CONSTRAINT %I',
            operation_constraint.conname
        );
    END LOOP;
END
$$;

ALTER TABLE checkout_sessions
    ADD CONSTRAINT ck_checkout_sessions_operation_type
        CHECK (
            operation_type IN (
                'BAR_COMMAND',
                'LAVA_WORK_ORDER'
            )
        );
