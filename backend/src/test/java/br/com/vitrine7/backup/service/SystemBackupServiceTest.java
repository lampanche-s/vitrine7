package br.com.vitrine7.backup.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SystemBackupServiceTest {

    @Test
    void parsesPostgresJdbcConnectionWithoutExposingPasswordInCommand() {
        SystemBackupService.PostgresConnection connection =
                SystemBackupService.parseJdbcUrl(
                        "jdbc:postgresql://127.0.0.1:5432/vitrine7_db?sslmode=require",
                        "vitrine7",
                        "secret"
                );

        assertEquals("127.0.0.1", connection.host());
        assertEquals(5432, connection.port());
        assertEquals("vitrine7_db", connection.database());
        assertEquals("vitrine7", connection.username());
        assertEquals("secret", connection.password());
        assertEquals("require", connection.sslMode());
    }

    @Test
    void usesDefaultPostgresPortAndBusinessTimeInFileName() {
        SystemBackupService.PostgresConnection connection =
                SystemBackupService.parseJdbcUrl(
                        "jdbc:postgresql://localhost/vitrine7_db",
                        "postgres",
                        null
                );

        assertEquals(5432, connection.port());
        assertNull(connection.sslMode());

        assertEquals(
                "vitrine7-backup-20260805-175600.backup",
                SystemBackupService.buildFileName(
                        Instant.parse("2026-08-05T20:56:00Z"),
                        ZoneId.of("America/Sao_Paulo")
                )
        );
    }
}
