package br.com.vitrine7.print.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class PrinterAgentHealthRepository {
    private final JdbcTemplate jdbc;

    public boolean heartbeat(
            String identity,
            String agentVersion,
            OffsetDateTime seenAt
    ) {
        return jdbc.update("""
                INSERT INTO printer_agent_heartbeats
                    (identity, agent_version, last_seen_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (identity) DO UPDATE
                SET agent_version = EXCLUDED.agent_version,
                    last_seen_at = EXCLUDED.last_seen_at,
                    updated_at = EXCLUDED.updated_at
                WHERE printer_agent_heartbeats.revoked_at IS NULL
                """, identity, agentVersion, seenAt, seenAt, seenAt) > 0;
    }

    public Snapshot latest() {
        return jdbc.query("""
                SELECT identity, agent_version, last_seen_at, revoked_at
                FROM printer_agent_heartbeats
                ORDER BY last_seen_at DESC
                LIMIT 1
                """, (rs, row) -> new Snapshot(
                rs.getString("identity"),
                rs.getString("agent_version"),
                rs.getObject("last_seen_at", OffsetDateTime.class),
                rs.getObject("revoked_at", OffsetDateTime.class)
        )).stream().findFirst().orElse(null);
    }

    public record Snapshot(
            String identity,
            String agentVersion,
            OffsetDateTime lastSeenAt,
            OffsetDateTime revokedAt
    ) {
    }
}
