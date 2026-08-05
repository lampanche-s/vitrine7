-- Preferências de modo administrativo e configurações editáveis deixaram
-- de fazer parte do produto. Os dados do estabelecimento passam a ser
-- fornecidos por variáveis de ambiente.
DROP TABLE user_preferences;
DROP TABLE system_settings;

-- Estrutura antiga, substituída por payment_provider_profiles.
DROP TABLE payment_terminal_settings;

-- Mantém as consultas de revogação por usuário eficientes.
CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_not_revoked
    ON auth_sessions (user_id, created_at DESC)
    WHERE revoked_at IS NULL;
