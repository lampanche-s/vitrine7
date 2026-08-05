-- O fluxo assíncrono de estorno pela maquininha foi substituído
-- por uma marcação simples e persistida no próprio pagamento.
UPDATE payments
SET
    status = 'APPROVED',
    reversed_at = NULL,
    reversed_by_user_id = NULL,
    reversal_reason = NULL,
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE status = 'REVERSAL_PENDING';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM payments
        WHERE status IN (
            'REFUND_REQUESTED',
            'REFUNDED'
        )
    ) THEN
        RAISE EXCEPTION
            'Existem pagamentos em estados de reembolso não suportados. A V40 foi interrompida.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM payment_terminal_commands
        WHERE command_type IN (
            'REVERSE_PAYMENT',
            'CANCEL_PAYMENT'
        )
    ) THEN
        RAISE EXCEPTION
            'Existem comandos antigos de estorno ou cancelamento da maquininha. A V40 foi interrompida.';
    END IF;
END
$$;

ALTER TABLE payments
    DROP CONSTRAINT ck_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'APPROVED',
                'DECLINED',
                'CANCELLED',
                'REVERSED'
            )
        );

ALTER TABLE payments
    DROP CONSTRAINT ck_payments_approved_data;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_approved_data
        CHECK (
            (
                status IN (
                    'APPROVED',
                    'REVERSED'
                )
                AND approved_at IS NOT NULL
            )
            OR
            (
                status NOT IN (
                    'APPROVED',
                    'REVERSED'
                )
                AND approved_at IS NULL
            )
        );

DROP INDEX uq_payments_settled_checkout;

CREATE UNIQUE INDEX uq_payments_settled_checkout
    ON payments (checkout_session_id)
    WHERE status IN (
        'APPROVED',
        'REVERSED'
    );

ALTER TABLE payment_terminal_commands
    DROP CONSTRAINT ck_payment_terminal_commands_type;

ALTER TABLE payment_terminal_commands
    ADD CONSTRAINT ck_payment_terminal_commands_type
        CHECK (
            command_type IN (
                'INITIATE_PAYMENT',
                'QUERY_PAYMENT'
            )
        );
