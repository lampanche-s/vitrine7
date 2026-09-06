package br.com.vitrine7.operations;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class V53MigrationTest {
    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void appliesAllMigrationsAndCreatesAdditivePrinterHeartbeatTable() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        ));
        assertEquals("53", jdbc.queryForObject("""
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """, String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'printer_agent_heartbeats'
                """, Integer.class));
        assertEquals(6, jdbc.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'printer_agent_heartbeats'
                """, Integer.class));
    }
}
