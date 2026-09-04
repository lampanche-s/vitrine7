\set ON_ERROR_STOP on
\pset pager off

BEGIN;

-- Corrige apenas a venda-placeholder que já existia antes desta carga.
UPDATE bar_tabs
SET name = 'Atendimento balcão',
    normalized_name = 'atendimento balcao',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1
  AND name = 'pppp';

UPDATE bar_tab_lines
SET item_name_snapshot = 'Lavagem externa expressa',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1
  AND item_name_snapshot = 'cu';

CREATE TEMP TABLE seed_users ON COMMIT DROP AS
SELECT id,
       ROW_NUMBER() OVER (ORDER BY id) AS row_number,
       COUNT(*) OVER () AS total
FROM users
WHERE deleted_at IS NULL
  AND status = 'ATIVO'
  AND role IN ('ADMINISTRADOR', 'OPERADOR');

CREATE TEMP TABLE seed_clients ON COMMIT DROP AS
SELECT id, name, vehicle_name, plate,
       ROW_NUMBER() OVER (ORDER BY id) AS row_number,
       COUNT(*) OVER () AS total
FROM clients
WHERE deleted_at IS NULL
  AND active = TRUE;

CREATE TEMP TABLE seed_items ON COMMIT DROP AS
SELECT id, name, price_cents,
       ROW_NUMBER() OVER (ORDER BY id) AS row_number,
       COUNT(*) OVER () AS total
FROM catalog_entries
WHERE deleted_at IS NULL
  AND entry_type = 'ITEM';

CREATE TEMP TABLE seed_services ON COMMIT DROP AS
SELECT id, name, price_cents,
       ROW_NUMBER() OVER (ORDER BY id) AS row_number,
       COUNT(*) OVER () AS total
FROM catalog_entries
WHERE deleted_at IS NULL
  AND entry_type = 'SERVICE';

CREATE TEMP TABLE seed_sales ON COMMIT DROP AS
WITH generated AS (
    SELECT sale_number,
           MD5('vitrine7-fake-sale-' || sale_number) AS sale_hash,
           MD5('vitrine7-fake-checkout-' || sale_number) AS checkout_hash,
           ((sale_number % 20) < 9) AS has_client,
           (
               ((sale_number % 20) < 9 AND ((sale_number * 7) % 10) < 7)
               OR
               ((sale_number % 20) >= 9 AND ((sale_number * 13) % 25) < 3)
           ) AS has_service,
           (
               CURRENT_DATE
               - ((sale_number * 37) % 420) * INTERVAL '1 day'
               + MAKE_INTERVAL(
                   hours => 8 + ((sale_number * 11) % 15),
                   mins => ((sale_number * 13) % 60)
               )
           )::timestamptz AS completed_at,
           1 + ((sale_number * 7) % 5) AS line_count
    FROM GENERATE_SERIES(1, 2400) AS generated_sale(sale_number)
)
SELECT generated.sale_number,
       (
           SUBSTR(generated.sale_hash, 1, 8) || '-' ||
           SUBSTR(generated.sale_hash, 9, 4) || '-4' ||
           SUBSTR(generated.sale_hash, 14, 3) || '-a' ||
           SUBSTR(generated.sale_hash, 18, 3) || '-' ||
           SUBSTR(generated.sale_hash, 21, 12)
       )::uuid AS tab_key,
       (
           SUBSTR(generated.checkout_hash, 1, 8) || '-' ||
           SUBSTR(generated.checkout_hash, 9, 4) || '-4' ||
           SUBSTR(generated.checkout_hash, 14, 3) || '-b' ||
           SUBSTR(generated.checkout_hash, 18, 3) || '-' ||
           SUBSTR(generated.checkout_hash, 21, 12)
       )::uuid AS checkout_id,
       generated.has_client,
       generated.has_service,
       generated.completed_at,
       generated.line_count,
       seed_users.id AS user_id,
       CASE WHEN generated.has_client THEN seed_clients.id END AS client_id,
       CASE WHEN generated.has_client THEN seed_clients.name END AS client_name,
       CASE WHEN generated.has_client THEN seed_clients.vehicle_name END AS client_vehicle,
       CASE WHEN generated.has_client THEN seed_clients.plate END AS client_plate
FROM generated
JOIN seed_users
  ON seed_users.row_number = ((generated.sale_number - 1) % seed_users.total) + 1
LEFT JOIN seed_clients
  ON generated.has_client
 AND seed_clients.row_number = ((generated.sale_number * 17 - 1) % seed_clients.total) + 1;

CREATE TEMP TABLE seed_lines ON COMMIT DROP AS
WITH expanded AS (
    SELECT sale.*,
           line_number,
           CASE
               WHEN sale.has_service AND line_number = 1 THEN 'SERVICE'
               ELSE 'ITEM'
           END AS entry_type
    FROM seed_sales sale
    CROSS JOIN LATERAL GENERATE_SERIES(1, sale.line_count) AS line(line_number)
), selected AS (
    SELECT expanded.sale_number,
           expanded.checkout_id,
           expanded.line_number,
           expanded.entry_type,
           COALESCE(service.id, item.id) AS catalog_entry_id,
           COALESCE(service.name, item.name) AS entry_name,
           COALESCE(service.price_cents, item.price_cents) AS unit_price_cents,
           CASE
               WHEN expanded.entry_type = 'SERVICE' THEN 1
               ELSE 1 + ((expanded.sale_number + expanded.line_number * 3) % 4)
           END AS quantity
    FROM expanded
    LEFT JOIN seed_items item
      ON expanded.entry_type = 'ITEM'
     AND item.row_number = (
         ((expanded.sale_number * 11 + expanded.line_number * 17 - 1) % item.total) + 1
     )
    LEFT JOIN seed_services service
      ON expanded.entry_type = 'SERVICE'
     AND service.row_number = (((expanded.sale_number * 7 - 1) % service.total) + 1)
)
SELECT *, unit_price_cents * quantity AS line_total_cents
FROM selected;

CREATE TEMP TABLE seed_totals ON COMMIT DROP AS
SELECT sale_number,
       SUM(line_total_cents)::bigint AS subtotal_cents,
       CASE
           WHEN sale_number % 10 = 0 THEN (SUM(line_total_cents) / 10)::bigint
           WHEN sale_number % 10 = 1 THEN (SUM(line_total_cents) / 20)::bigint
           ELSE 0
       END AS discount_cents
FROM seed_lines
GROUP BY sale_number;

INSERT INTO checkout_sessions(
    id, idempotency_key, request_fingerprint,
    business_area, operation_type, source_id, status,
    subtotal_cents, discount_cents, total_cents,
    document_type, expires_at, paid_at, finalized_at,
    created_by_user_id, created_at, updated_at
)
SELECT sale.checkout_id,
       sale.checkout_id,
       MD5('checkout-request-' || sale.sale_number) || MD5('checkout-fingerprint-' || sale.sale_number),
       'BAR', 'BAR_COMMAND', NULL, 'FINALIZED',
       totals.subtotal_cents,
       totals.discount_cents,
       totals.subtotal_cents - totals.discount_cents,
       'GENERAL_RECEIPT',
       sale.completed_at + INTERVAL '3 hours',
       sale.completed_at - INTERVAL '2 minutes',
       sale.completed_at,
       sale.user_id,
       sale.completed_at - INTERVAL '50 minutes',
       sale.completed_at
FROM seed_sales sale
JOIN seed_totals totals USING (sale_number)
ON CONFLICT (id) DO NOTHING;

INSERT INTO bar_tabs(
    name, normalized_name, status, checkout_session_id,
    subtotal_cents, discount_cents, total_cents,
    create_idempotency_key, create_request_fingerprint,
    prepare_idempotency_key, prepare_request_fingerprint, prepared_at,
    created_by_user_id, created_at, updated_at, closed_at,
    client_id, vehicle_name_snapshot, vehicle_plate_snapshot
)
SELECT CASE
           WHEN sale.has_client THEN 'Atendimento - ' || sale.client_name
           WHEN sale.has_service THEN
               (ARRAY[
                   'Lavagem avulsa - compacto',
                   'Lavagem avulsa - sedã',
                   'Lavagem avulsa - SUV',
                   'Estética automotiva avulsa'
               ])[1 + (sale.sale_number % 4)]
           ELSE
               (ARRAY[
                   'Mesa varanda', 'Mesa salão', 'Balcão principal',
                   'Retirada balcão', 'Pedido para viagem', 'Área externa'
               ])[1 + (sale.sale_number % 6)]
       END AS name,
       LOWER(TRANSLATE(
           CASE
               WHEN sale.has_client THEN 'Atendimento - ' || sale.client_name
               WHEN sale.has_service THEN 'Lavagem avulsa'
               ELSE 'Venda avulsa'
           END,
           'áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ',
           'aaaaeeiooouucAAAAEEIOOOUUC'
       )) AS normalized_name,
       'CLOSED', sale.checkout_id,
       totals.subtotal_cents,
       totals.discount_cents,
       totals.subtotal_cents - totals.discount_cents,
       sale.tab_key,
       MD5('tab-request-' || sale.sale_number) || MD5('tab-fingerprint-' || sale.sale_number),
       (
           SUBSTR(MD5('prepare-' || sale.sale_number), 1, 8) || '-' ||
           SUBSTR(MD5('prepare-' || sale.sale_number), 9, 4) || '-4' ||
           SUBSTR(MD5('prepare-' || sale.sale_number), 14, 3) || '-b' ||
           SUBSTR(MD5('prepare-' || sale.sale_number), 18, 3) || '-' ||
           SUBSTR(MD5('prepare-' || sale.sale_number), 21, 12)
       )::uuid,
       MD5('prepare-request-' || sale.sale_number) || MD5('prepare-fingerprint-' || sale.sale_number),
       sale.completed_at - INTERVAL '12 minutes',
       sale.user_id,
       sale.completed_at - INTERVAL '50 minutes',
       sale.completed_at,
       sale.completed_at,
       sale.client_id,
       CASE
           WHEN sale.has_client THEN sale.client_vehicle
           WHEN sale.has_service THEN
               (ARRAY[
                   'Chevrolet Onix prata', 'Hyundai HB20 branco',
                   'Jeep Renegade preto', 'Fiat Strada cinza'
               ])[1 + (sale.sale_number % 4)]
       END,
       CASE
           WHEN sale.has_client THEN sale.client_plate
           WHEN sale.has_service THEN 'VTR' || LPAD((sale.sale_number % 10000)::text, 4, '0')
       END
FROM seed_sales sale
JOIN seed_totals totals USING (sale_number)
ON CONFLICT (create_idempotency_key) DO NOTHING;

UPDATE checkout_sessions checkout
SET source_id = tab.id
FROM bar_tabs tab
JOIN seed_sales sale ON sale.tab_key = tab.create_idempotency_key
WHERE checkout.id = sale.checkout_id
  AND checkout.source_id IS DISTINCT FROM tab.id;

INSERT INTO bar_tab_lines(
    tab_id, item_name_snapshot, unit_price_cents,
    quantity, line_total_cents, catalog_entry_id,
    entry_type_snapshot, created_at, updated_at
)
SELECT tab.id,
       line.entry_name,
       line.unit_price_cents,
       line.quantity,
       line.line_total_cents,
       line.catalog_entry_id,
       line.entry_type,
       sale.completed_at - INTERVAL '35 minutes' + line.line_number * INTERVAL '2 minutes',
       sale.completed_at - INTERVAL '20 minutes'
FROM seed_lines line
JOIN seed_sales sale USING (sale_number)
JOIN bar_tabs tab ON tab.create_idempotency_key = sale.tab_key
ON CONFLICT (tab_id, catalog_entry_id) DO NOTHING;

-- Pagamentos únicos, incluindo pequena parcela de estornos realistas.
WITH payment_data AS (
    SELECT sale.*,
           totals.subtotal_cents - totals.discount_cents AS amount_cents,
           CASE
               WHEN sale.sale_number % 100 < 30 THEN 'PIX'
               WHEN sale.sale_number % 100 < 55 THEN 'CASH'
               WHEN sale.sale_number % 100 < 77 THEN 'DEBIT_CARD'
               ELSE 'CREDIT_CARD'
           END AS method,
           (sale.sale_number % 50 = 0) AS reversed
    FROM seed_sales sale
    JOIN seed_totals totals USING (sale_number)
    WHERE sale.sale_number % 100 < 95
)
INSERT INTO payments(
    id, checkout_session_id, idempotency_key, request_fingerprint,
    method, processing_mode, status, amount_cents,
    cash_received_cents, cash_change_cents, cash_confirmed_by_user_id,
    approved_at, approved_by_user_id,
    reversed_at, reversed_by_user_id, reversal_reason,
    created_by_user_id, created_at, updated_at
)
SELECT (
           SUBSTR(MD5('payment-' || sale_number), 1, 8) || '-' ||
           SUBSTR(MD5('payment-' || sale_number), 9, 4) || '-4' ||
           SUBSTR(MD5('payment-' || sale_number), 14, 3) || '-a' ||
           SUBSTR(MD5('payment-' || sale_number), 18, 3) || '-' ||
           SUBSTR(MD5('payment-' || sale_number), 21, 12)
       )::uuid,
       checkout_id,
       (
           SUBSTR(MD5('payment-key-' || sale_number), 1, 8) || '-' ||
           SUBSTR(MD5('payment-key-' || sale_number), 9, 4) || '-4' ||
           SUBSTR(MD5('payment-key-' || sale_number), 14, 3) || '-b' ||
           SUBSTR(MD5('payment-key-' || sale_number), 18, 3) || '-' ||
           SUBSTR(MD5('payment-key-' || sale_number), 21, 12)
       )::uuid,
       MD5('payment-request-' || sale_number) || MD5('payment-fingerprint-' || sale_number),
       method,
       CASE WHEN method = 'CASH' THEN 'CASH' ELSE 'TERMINAL_SIMULATED' END,
       CASE WHEN reversed THEN 'REVERSED' ELSE 'APPROVED' END,
       amount_cents,
       CASE WHEN method = 'CASH' THEN CEIL(amount_cents / 1000.0)::bigint * 1000 END,
       CASE WHEN method = 'CASH' THEN CEIL(amount_cents / 1000.0)::bigint * 1000 - amount_cents END,
       CASE WHEN method = 'CASH' THEN user_id END,
       completed_at - INTERVAL '3 minutes',
       user_id,
       CASE WHEN reversed THEN completed_at + INTERVAL '2 days' END,
       CASE WHEN reversed THEN user_id END,
       CASE WHEN reversed THEN 'Solicitação do cliente após conferência' END,
       user_id,
       completed_at - INTERVAL '5 minutes',
       CASE WHEN reversed THEN completed_at + INTERVAL '2 days' ELSE completed_at END
FROM payment_data
ON CONFLICT (idempotency_key) DO NOTHING;

-- Cinco por cento das operações recebem dois pagamentos aprovados.
WITH split_data AS (
    SELECT sale.*,
           totals.subtotal_cents - totals.discount_cents AS total_cents,
           ((totals.subtotal_cents - totals.discount_cents) * 60 / 100)::bigint AS first_amount
    FROM seed_sales sale
    JOIN seed_totals totals USING (sale_number)
    WHERE sale.sale_number % 100 >= 95
), split_payments AS (
    SELECT split_data.*,
           part,
           CASE
               WHEN part = 1 THEN 'PIX'
               WHEN sale_number % 2 = 0 THEN 'CREDIT_CARD'
               ELSE 'DEBIT_CARD'
           END AS method,
           CASE WHEN part = 1 THEN first_amount ELSE total_cents - first_amount END AS amount_cents
    FROM split_data
    CROSS JOIN GENERATE_SERIES(1, 2) AS payment_part(part)
)
INSERT INTO payments(
    id, checkout_session_id, idempotency_key, request_fingerprint,
    method, processing_mode, status, amount_cents,
    approved_at, approved_by_user_id,
    created_by_user_id, created_at, updated_at
)
SELECT (
           SUBSTR(MD5('split-payment-' || sale_number || '-' || part), 1, 8) || '-' ||
           SUBSTR(MD5('split-payment-' || sale_number || '-' || part), 9, 4) || '-4' ||
           SUBSTR(MD5('split-payment-' || sale_number || '-' || part), 14, 3) || '-a' ||
           SUBSTR(MD5('split-payment-' || sale_number || '-' || part), 18, 3) || '-' ||
           SUBSTR(MD5('split-payment-' || sale_number || '-' || part), 21, 12)
       )::uuid,
       checkout_id,
       (
           SUBSTR(MD5('split-key-' || sale_number || '-' || part), 1, 8) || '-' ||
           SUBSTR(MD5('split-key-' || sale_number || '-' || part), 9, 4) || '-4' ||
           SUBSTR(MD5('split-key-' || sale_number || '-' || part), 14, 3) || '-b' ||
           SUBSTR(MD5('split-key-' || sale_number || '-' || part), 18, 3) || '-' ||
           SUBSTR(MD5('split-key-' || sale_number || '-' || part), 21, 12)
       )::uuid,
       MD5('split-request-' || sale_number || '-' || part) ||
       MD5('split-fingerprint-' || sale_number || '-' || part),
       method,
       'TERMINAL_SIMULATED',
       'APPROVED',
       amount_cents,
       completed_at - (4 - part) * INTERVAL '1 minute',
       user_id,
       user_id,
       completed_at - (6 - part) * INTERVAL '1 minute',
       completed_at
FROM split_payments
ON CONFLICT (idempotency_key) DO NOTHING;

-- Mantém estornos recentes no passado quando a data gerada cai no dia atual.
UPDATE payments payment
SET reversed_at = CURRENT_TIMESTAMP - INTERVAL '15 minutes',
    updated_at = CURRENT_TIMESTAMP - INTERVAL '15 minutes'
FROM seed_sales sale
WHERE payment.checkout_session_id = sale.checkout_id
  AND payment.status = 'REVERSED'
  AND payment.reversed_at > CURRENT_TIMESTAMP;

COMMIT;

WITH history AS (
    SELECT tab.id,
           (tab.client_id IS NOT NULL) AS linked_client,
           COUNT(payment.id) AS payment_count,
           BOOL_OR(payment.status = 'REVERSED') AS reversed
    FROM bar_tabs tab
    JOIN checkout_sessions checkout ON checkout.id = tab.checkout_session_id
    JOIN payments payment ON payment.checkout_session_id = checkout.id
    WHERE tab.status = 'CLOSED'
      AND checkout.status = 'FINALIZED'
      AND payment.status IN ('APPROVED', 'REVERSED')
    GROUP BY tab.id, tab.client_id
)
SELECT COUNT(*) AS vendas_no_historico,
       COUNT(*) FILTER (WHERE linked_client) AS com_cliente,
       COUNT(*) FILTER (WHERE NOT linked_client) AS avulsas,
       COUNT(*) FILTER (WHERE payment_count > 1) AS pagamentos_divididos,
       COUNT(*) FILTER (WHERE reversed) AS estornadas
FROM history;
