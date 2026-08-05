ALTER TABLE payment_terminal_commands
    DROP CONSTRAINT ck_payment_terminal_commands_type;

ALTER TABLE payment_terminal_commands
    ADD CONSTRAINT ck_payment_terminal_commands_type
        CHECK (
            command_type IN (
                'INITIATE_PAYMENT',
                'QUERY_PAYMENT',
                'CANCEL_PAYMENT',
                'REVERSE_PAYMENT'
            )
        );
