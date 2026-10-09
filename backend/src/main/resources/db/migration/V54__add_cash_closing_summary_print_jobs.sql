ALTER TABLE print_jobs
    DROP CONSTRAINT ck_print_jobs_document_kind;

ALTER TABLE print_jobs
    ADD CONSTRAINT ck_print_jobs_document_kind
        CHECK (document_kind IN (
            'RECEIPT',
            'PREPAYMENT_NOTE',
            'CASH_CLOSING',
            'CASH_CLOSING_SUMMARY',
            'ITEM_ORDER',
            'SERVICE_ORDER'
        ));

ALTER TABLE print_jobs
    DROP CONSTRAINT ck_print_jobs_reference;

ALTER TABLE print_jobs
    ADD CONSTRAINT ck_print_jobs_reference
        CHECK (
            (
                document_kind = 'RECEIPT'
                AND checkout_session_id IS NOT NULL
                AND bar_tab_id IS NULL
                AND business_date IS NULL
            )
            OR
            (
                document_kind = 'PREPAYMENT_NOTE'
                AND business_date IS NULL
                AND (
                    (
                        checkout_session_id IS NOT NULL
                        AND bar_tab_id IS NULL
                    )
                    OR
                    (
                        checkout_session_id IS NULL
                        AND bar_tab_id IS NOT NULL
                    )
                )
            )
            OR
            (
                document_kind IN ('CASH_CLOSING', 'CASH_CLOSING_SUMMARY')
                AND checkout_session_id IS NULL
                AND bar_tab_id IS NULL
                AND business_date IS NOT NULL
                AND requested_by_user_id IS NOT NULL
            )
            OR
            (
                document_kind IN ('ITEM_ORDER', 'SERVICE_ORDER')
                AND checkout_session_id IS NULL
                AND bar_tab_id IS NOT NULL
                AND business_date IS NULL
            )
        );

CREATE UNIQUE INDEX uq_print_jobs_active_cash_closing_summary
    ON print_jobs (requested_by_user_id, business_date, document_kind)
    WHERE document_kind = 'CASH_CLOSING_SUMMARY'
      AND status IN ('PENDING', 'PRINTING');
