ALTER TABLE print_jobs
    ADD COLUMN bar_tab_id BIGINT;

ALTER TABLE print_jobs
    ADD CONSTRAINT fk_print_jobs_bar_tab
        FOREIGN KEY (bar_tab_id)
        REFERENCES bar_tabs (id)
        ON DELETE CASCADE;

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
                document_kind = 'CASH_CLOSING'
                AND checkout_session_id IS NULL
                AND bar_tab_id IS NULL
                AND business_date IS NOT NULL
                AND requested_by_user_id IS NOT NULL
            )
        );
