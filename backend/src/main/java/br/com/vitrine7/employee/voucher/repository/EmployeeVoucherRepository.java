package br.com.vitrine7.employee.voucher.repository;

import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class EmployeeVoucherRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public EmployeeVoucherRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long count(Long employeeId, OffsetDateTime from, OffsetDateTime to) {
        return Optional.ofNullable(jdbc.queryForObject(
                "SELECT COUNT(*) FROM bar_tabs tab WHERE " + filters(), params(employeeId, from, to), Long.class
        )).orElse(0L);
    }

    public List<Long> pageIds(Long employeeId, OffsetDateTime from, OffsetDateTime to, int size, long offset) {
        MapSqlParameterSource parameters = params(employeeId, from, to).addValue("size", size).addValue("offset", offset);
        return jdbc.query("SELECT tab.id FROM bar_tabs tab WHERE " + filters()
                        + " ORDER BY tab.closed_at DESC, tab.id DESC LIMIT :size OFFSET :offset",
                parameters, (rs, rowNum) -> rs.getLong(1));
    }

    public List<Row> rowsByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT tab.id operation_id, tab.employee_id, employee.name employee_name,
                       tab.name registered_tab_name, tab.closed_at, tab.total_cents,
                       tab.created_by_user_id operator_user_id, creator.name operator_name,
                       line.catalog_entry_id, line.item_name_snapshot, line.entry_type_snapshot,
                       line.quantity, line.unit_price_cents, line.line_total_cents
                FROM bar_tabs tab
                JOIN employees employee ON employee.id = tab.employee_id
                JOIN users creator ON creator.id = tab.created_by_user_id
                JOIN bar_tab_lines line ON line.tab_id = tab.id
                WHERE tab.id IN (:ids)
                ORDER BY tab.closed_at DESC, tab.id DESC, line.id ASC
                """, new MapSqlParameterSource("ids", ids), (rs, rowNum) -> new Row(
                rs.getLong("operation_id"), rs.getLong("employee_id"), rs.getString("employee_name"),
                rs.getString("registered_tab_name"), rs.getObject("closed_at", OffsetDateTime.class),
                rs.getLong("total_cents"), rs.getLong("operator_user_id"), rs.getString("operator_name"),
                rs.getLong("catalog_entry_id"), rs.getString("item_name_snapshot"),
                rs.getString("entry_type_snapshot"), rs.getInt("quantity"),
                rs.getLong("unit_price_cents"), rs.getLong("line_total_cents")));
    }

    public List<Row> reportRows(Long employeeId, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.query("""
                SELECT tab.id operation_id, tab.employee_id, employee.name employee_name,
                       tab.name registered_tab_name, tab.closed_at, tab.total_cents,
                       tab.created_by_user_id operator_user_id, creator.name operator_name,
                       line.catalog_entry_id, line.item_name_snapshot, line.entry_type_snapshot,
                       line.quantity, line.unit_price_cents, line.line_total_cents
                FROM bar_tabs tab
                JOIN employees employee ON employee.id = tab.employee_id
                JOIN users creator ON creator.id = tab.created_by_user_id
                JOIN bar_tab_lines line ON line.tab_id = tab.id
                """ + "WHERE " + filters() + " ORDER BY tab.closed_at ASC, tab.id ASC, line.id ASC",
                params(employeeId, from, to), (rs, rowNum) -> new Row(
                        rs.getLong("operation_id"), rs.getLong("employee_id"), rs.getString("employee_name"),
                        rs.getString("registered_tab_name"), rs.getObject("closed_at", OffsetDateTime.class),
                        rs.getLong("total_cents"), rs.getLong("operator_user_id"), rs.getString("operator_name"),
                        rs.getLong("catalog_entry_id"), rs.getString("item_name_snapshot"),
                        rs.getString("entry_type_snapshot"), rs.getInt("quantity"),
                        rs.getLong("unit_price_cents"), rs.getLong("line_total_cents")));
    }

    private String filters() {
        return "tab.status = 'CLOSED' AND tab.closure_type = 'VOUCHER' AND tab.employee_id IS NOT NULL "
                + "AND (CAST(:employeeId AS BIGINT) IS NULL OR tab.employee_id = :employeeId) "
                + "AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR tab.closed_at >= :fromDate) "
                + "AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR tab.closed_at < :toDate)";
    }

    private MapSqlParameterSource params(Long employeeId, OffsetDateTime from, OffsetDateTime to) {
        return new MapSqlParameterSource().addValue("employeeId", employeeId)
                .addValue("fromDate", from).addValue("toDate", to);
    }

    public record Row(long operationId, long employeeId, String employeeName, String registeredTabName,
                      OffsetDateTime closedAt, long totalCents, long operatorUserId, String operatorName,
                      long catalogEntryId, String itemName, String type, int quantity,
                      long unitPriceCents, long lineTotalCents) {}
}
