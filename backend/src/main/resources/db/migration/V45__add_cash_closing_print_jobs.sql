ALTER TABLE print_jobs
    ALTER COLUMN checkout_session_id DROP NOT NULL;

ALTER TABLE print_jobs
    ADD COLUMN business_date DATE;

ALTER TABLE print_jobs
    DROP CONSTRAINT IF EXISTS ck_print_jobs_document_kind;

ALTER TABLE print_jobs
    ADD CONSTRAINT ck_print_jobs_document_kind
        CHECK (document_kind IN ('RECEIPT', 'PREPAYMENT_NOTE', 'CASH_CLOSING'));

ALTER TABLE print_jobs
    ADD CONSTRAINT ck_print_jobs_reference
        CHECK (
            (
                document_kind IN ('RECEIPT', 'PREPAYMENT_NOTE')
                AND checkout_session_id IS NOT NULL
                AND business_date IS NULL
            )
            OR
            (
                document_kind = 'CASH_CLOSING'
                AND checkout_session_id IS NULL
                AND business_date IS NOT NULL
                AND requested_by_user_id IS NOT NULL
            )
        );

CREATE UNIQUE INDEX uq_print_jobs_active_cash_closing
    ON print_jobs (requested_by_user_id, business_date, document_kind)
    WHERE document_kind = 'CASH_CLOSING'
      AND status IN ('PENDING', 'PRINTING');
