package br.com.vitrine7.employee.voucher.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class EmployeeVoucherRepositoryTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private JdbcTemplate jdbc;
    private EmployeeVoucherRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        repository = new EmployeeVoucherRepository(new NamedParameterJdbcTemplate(dataSource));

        jdbc.execute("DROP TABLE IF EXISTS bar_tab_lines, bar_tabs, employees, users CASCADE");
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(80) NOT NULL)");
        jdbc.execute("CREATE TABLE employees (id BIGINT PRIMARY KEY, name VARCHAR(80) NOT NULL)");
        jdbc.execute("""
                CREATE TABLE bar_tabs (
                    id BIGINT PRIMARY KEY,
                    employee_id BIGINT,
                    name VARCHAR(80) NOT NULL,
                    closed_at TIMESTAMPTZ,
                    total_cents BIGINT NOT NULL,
                    created_by_user_id BIGINT NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    closure_type VARCHAR(20)
                )
                """);
        jdbc.execute("""
                CREATE TABLE bar_tab_lines (
                    id BIGINT PRIMARY KEY,
                    tab_id BIGINT NOT NULL,
                    catalog_entry_id BIGINT NOT NULL,
                    item_name_snapshot VARCHAR(120) NOT NULL,
                    entry_type_snapshot VARCHAR(30) NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price_cents BIGINT NOT NULL,
                    line_total_cents BIGINT NOT NULL
                )
                """);
    }

    @Test
    void reportRowsReturnsClosedVoucherTabWithLineWithoutCheckoutOrPayment() {
        OffsetDateTime closedAt = OffsetDateTime.of(2026, 8, 27, 12, 0, 0, 0, ZoneOffset.UTC);
        jdbc.update("INSERT INTO users (id, name) VALUES (1, 'Operador')");
        jdbc.update("INSERT INTO employees (id, name) VALUES (10, 'Funcionário')");
        jdbc.update("""
                INSERT INTO bar_tabs (id, employee_id, name, closed_at, total_cents, created_by_user_id, status, closure_type)
                VALUES (100, 10, 'Comanda vale', ?, 2500, 1, 'CLOSED', 'VOUCHER')
                """, closedAt);
        jdbc.update("""
                INSERT INTO bar_tab_lines (id, tab_id, catalog_entry_id, item_name_snapshot, entry_type_snapshot,
                                           quantity, unit_price_cents, line_total_cents)
                VALUES (1000, 100, 50, 'Produto', 'PRODUCT', 1, 2500, 2500)
                """);

        var rows = repository.reportRows(null, closedAt.minusHours(1), closedAt.plusHours(1));

        assertEquals(1, rows.size());
        assertEquals(100L, rows.get(0).operationId());
        assertEquals(10L, rows.get(0).employeeId());
        assertEquals(50L, rows.get(0).catalogEntryId());
    }
}
