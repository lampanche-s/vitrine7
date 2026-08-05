UPDATE checkout_sessions
SET document_type = 'GENERAL_RECEIPT',
    cpf_digits = NULL
WHERE document_type IN (
          'NFCE_WITHOUT_CPF',
          'NFCE_WITH_CPF'
      )
   OR cpf_digits IS NOT NULL;

ALTER TABLE checkout_sessions
    DROP CONSTRAINT IF EXISTS ck_checkout_sessions_document_type,
    DROP CONSTRAINT IF EXISTS ck_checkout_sessions_cpf;

ALTER TABLE checkout_sessions
    ADD CONSTRAINT ck_checkout_sessions_document_type
        CHECK (
            document_type IS NULL
            OR document_type = 'GENERAL_RECEIPT'
        ),
    ADD CONSTRAINT ck_checkout_sessions_cpf
        CHECK (cpf_digits IS NULL);
