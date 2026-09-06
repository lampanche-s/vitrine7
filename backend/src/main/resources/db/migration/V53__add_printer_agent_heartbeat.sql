CREATE TABLE printer_agent_heartbeats (
    identity VARCHAR(120) PRIMARY KEY,
    agent_version VARCHAR(60) NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_printer_agent_heartbeats_identity
        CHECK (char_length(trim(identity)) BETWEEN 1 AND 120),
    CONSTRAINT ck_printer_agent_heartbeats_version
        CHECK (char_length(trim(agent_version)) BETWEEN 1 AND 60)
);

CREATE INDEX idx_printer_agent_heartbeats_last_seen
    ON printer_agent_heartbeats (last_seen_at DESC);
