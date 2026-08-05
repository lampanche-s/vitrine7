package br.com.vitrine7.report.repository;

import br.com.vitrine7.report.dto.SalesReportCatalogEntryResponse;
import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportLineResponse;
import br.com.vitrine7.report.dto.SalesReportOperationResponse;
import br.com.vitrine7.report.dto.SalesReportPaymentBreakdownResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class SalesReportReadRepository {

    private static final String OPERATION_CTE = """
            WITH latest_payment AS (
                SELECT DISTINCT ON (payment.checkout_session_id)
                    payment.checkout_session_id,
                    payment.method,
                    payment.status,
                    payment.approved_at,
                    payment.approved_by_user_id,
                    payment.created_by_user_id
                FROM payments payment
                WHERE payment.status IN (
                    'APPROVED',
                    'REVERSED'
                )
                ORDER BY
                    payment.checkout_session_id,
                    COALESCE(
                        payment.reversed_at,
                        payment.approved_at,
                        payment.updated_at,
                        payment.created_at
                    ) DESC,
                    payment.created_at DESC
            ),
            eligible_commands AS (
                SELECT
                    tab.id AS operation_id,
                    tab.name AS display_name,
                    tab.subtotal_cents,
                    tab.discount_cents,
                    tab.total_cents,
                    COALESCE(
                        tab.closed_at,
                        checkout.finalized_at,
                        payment.approved_at,
                        tab.updated_at
                    ) AS completed_at,
                    payment.method AS payment_method,
                    COALESCE(
                        responsible.name,
                        creator.name
                    ) AS responsible_user_name
                FROM bar_tabs tab
                JOIN checkout_sessions checkout
                    ON checkout.id = tab.checkout_session_id
                   AND checkout.operation_type = 'BAR_COMMAND'
                JOIN latest_payment payment
                    ON payment.checkout_session_id = checkout.id
                LEFT JOIN users responsible
                    ON responsible.id = COALESCE(
                        payment.approved_by_user_id,
                        payment.created_by_user_id
                    )
                LEFT JOIN users creator
                    ON creator.id = tab.created_by_user_id
                WHERE tab.status = 'CLOSED'
                  AND checkout.status = 'FINALIZED'
                  AND payment.status = 'APPROVED'
                  AND (
                      CAST(:fromInstant AS timestamptz) IS NULL
                      OR COALESCE(
                          tab.closed_at,
                          checkout.finalized_at,
                          payment.approved_at,
                          tab.updated_at
                      ) >= CAST(:fromInstant AS timestamptz)
                  )
                  AND (
                      CAST(:toInstant AS timestamptz) IS NULL
                      OR COALESCE(
                          tab.closed_at,
                          checkout.finalized_at,
                          payment.approved_at,
                          tab.updated_at
                      ) < CAST(:toInstant AS timestamptz)
                  )
            ),
            line_totals AS (
                SELECT
                    line.tab_id,
                    COALESCE(SUM(line.line_total_cents), 0)::bigint
                        AS all_gross_cents,
                    COALESCE(SUM(line.line_total_cents)
                        FILTER (WHERE line.entry_type_snapshot = 'ITEM'), 0)::bigint
                        AS item_gross_cents,
                    COALESCE(SUM(line.line_total_cents)
                        FILTER (WHERE line.entry_type_snapshot = 'SERVICE'), 0)::bigint
                        AS service_gross_cents,
                    COUNT(*)::integer AS all_line_count,
                    COUNT(*) FILTER (WHERE line.entry_type_snapshot = 'ITEM')::integer
                        AS item_line_count,
                    COUNT(*) FILTER (WHERE line.entry_type_snapshot = 'SERVICE')::integer
                        AS service_line_count,
                    COALESCE(SUM(line.quantity), 0)::integer AS all_units,
                    COALESCE(SUM(line.quantity)
                        FILTER (WHERE line.entry_type_snapshot = 'ITEM'), 0)::integer
                        AS item_units,
                    COALESCE(SUM(line.quantity)
                        FILTER (WHERE line.entry_type_snapshot = 'SERVICE'), 0)::integer
                        AS service_units
                FROM bar_tab_lines line
                GROUP BY line.tab_id
            ),
            operation_scope AS (
                SELECT
                    command.operation_id,
                    command.display_name,
                    command.completed_at,
                    command.payment_method,
                    command.responsible_user_name,
                    CASE CAST(:scope AS text)
                        WHEN 'ITEM' THEN totals.item_gross_cents
                        WHEN 'SERVICE' THEN totals.service_gross_cents
                        ELSE totals.all_gross_cents
                    END AS gross_cents,
                    CASE CAST(:scope AS text)
                        WHEN 'SERVICE' THEN ROUND(
                            command.discount_cents::numeric
                            * totals.service_gross_cents::numeric
                            / NULLIF(totals.all_gross_cents, 0)
                        )::bigint
                        WHEN 'ITEM' THEN command.discount_cents - ROUND(
                            command.discount_cents::numeric
                            * totals.service_gross_cents::numeric
                            / NULLIF(totals.all_gross_cents, 0)
                        )::bigint
                        ELSE command.discount_cents
                    END AS allocated_discount_cents,
                    CASE CAST(:scope AS text)
                        WHEN 'ITEM' THEN totals.item_line_count
                        WHEN 'SERVICE' THEN totals.service_line_count
                        ELSE totals.all_line_count
                    END AS line_count,
                    CASE CAST(:scope AS text)
                        WHEN 'ITEM' THEN totals.item_units
                        WHEN 'SERVICE' THEN totals.service_units
                        ELSE totals.all_units
                    END AS total_units
                FROM eligible_commands command
                JOIN line_totals totals
                    ON totals.tab_id = command.operation_id
                WHERE CASE CAST(:scope AS text)
                    WHEN 'ITEM' THEN totals.item_gross_cents
                    WHEN 'SERVICE' THEN totals.service_gross_cents
                    ELSE totals.all_gross_cents
                END > 0
            )
            """;

    private static final String LINE_CTE = OPERATION_CTE + """
            , selected_lines AS (
                SELECT
                    operation.operation_id,
                    operation.display_name,
                    operation.completed_at,
                    line.item_name_snapshot AS item_name,
                    line.entry_type_snapshot AS entry_type,
                    line.quantity,
                    line.unit_price_cents,
                    line.line_total_cents
                FROM operation_scope operation
                JOIN bar_tab_lines line
                    ON line.tab_id = operation.operation_id
                WHERE CAST(:scope AS text) = 'ALL'
                   OR line.entry_type_snapshot = CAST(:scope AS text)
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SalesReportReadRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Totals totals(SalesReportFilters filters) {
        Totals totals = jdbcTemplate.queryForObject(
                OPERATION_CTE + """
                        SELECT
                            COALESCE(SUM(gross_cents - allocated_discount_cents), 0)::bigint
                                AS total_received_cents,
                            COUNT(*)::bigint AS operation_count,
                            COALESCE(SUM(total_units), 0)::bigint AS total_units
                        FROM operation_scope
                        """,
                params(filters),
                (resultSet, rowNumber) -> new Totals(
                        resultSet.getLong("total_received_cents"),
                        resultSet.getLong("operation_count"),
                        resultSet.getLong("total_units")
                )
        );

        return totals == null ? new Totals(0L, 0L, 0L) : totals;
    }

    public List<SalesReportPaymentBreakdownResponse> paymentBreakdown(
            SalesReportFilters filters
    ) {
        return jdbcTemplate.query(
                OPERATION_CTE + """
                        SELECT
                            payment_method,
                            COALESCE(SUM(gross_cents - allocated_discount_cents), 0)::bigint
                                AS amount_cents,
                            COUNT(*)::bigint AS operation_count
                        FROM operation_scope
                        GROUP BY payment_method
                        ORDER BY amount_cents DESC, payment_method ASC
                        """,
                params(filters),
                (resultSet, rowNumber) -> new SalesReportPaymentBreakdownResponse(
                        resultSet.getString("payment_method"),
                        resultSet.getLong("amount_cents"),
                        resultSet.getLong("operation_count")
                )
        );
    }

    public List<SalesReportCatalogEntryResponse> topEntries(
            SalesReportFilters filters,
            int limit
    ) {
        return jdbcTemplate.query(
                LINE_CTE + """
                        SELECT
                            item_name,
                            entry_type,
                            COALESCE(SUM(quantity), 0)::bigint AS quantity,
                            COALESCE(SUM(line_total_cents), 0)::bigint AS gross_cents
                        FROM selected_lines
                        GROUP BY item_name, entry_type
                        ORDER BY gross_cents DESC, quantity DESC, item_name ASC
                        LIMIT :limit
                        """,
                params(filters).addValue("limit", limit),
                (resultSet, rowNumber) -> new SalesReportCatalogEntryResponse(
                        resultSet.getString("item_name"),
                        resultSet.getString("entry_type"),
                        resultSet.getLong("quantity"),
                        resultSet.getLong("gross_cents")
                )
        );
    }

    public List<SalesReportOperationResponse> operations(
            SalesReportFilters filters,
            Integer limit
    ) {
        String limitClause = limit == null ? "" : " LIMIT :limit";
        MapSqlParameterSource params = params(filters);
        if (limit != null) {
            params.addValue("limit", limit);
        }

        return jdbcTemplate.query(
                OPERATION_CTE + """
                        SELECT
                            operation_id,
                            display_name,
                            completed_at,
                            payment_method,
                            responsible_user_name,
                            gross_cents,
                            allocated_discount_cents,
                            gross_cents - allocated_discount_cents AS net_cents,
                            line_count,
                            total_units
                        FROM operation_scope
                        ORDER BY completed_at DESC, operation_id DESC
                        """ + limitClause,
                params,
                new OperationRowMapper()
        );
    }

    public List<SalesReportLineResponse> lines(SalesReportFilters filters) {
        return jdbcTemplate.query(
                LINE_CTE + """
                        SELECT
                            operation_id,
                            display_name,
                            completed_at,
                            item_name,
                            entry_type,
                            quantity,
                            unit_price_cents,
                            line_total_cents
                        FROM selected_lines
                        ORDER BY completed_at DESC, operation_id DESC, item_name ASC
                        """,
                params(filters),
                (resultSet, rowNumber) -> new SalesReportLineResponse(
                        resultSet.getLong("operation_id"),
                        resultSet.getString("display_name"),
                        resultSet.getObject("completed_at", OffsetDateTime.class),
                        resultSet.getString("item_name"),
                        resultSet.getString("entry_type"),
                        resultSet.getInt("quantity"),
                        resultSet.getLong("unit_price_cents"),
                        resultSet.getLong("line_total_cents")
                )
        );
    }

    private MapSqlParameterSource params(SalesReportFilters filters) {
        return new MapSqlParameterSource()
                .addValue(
                        "fromInstant",
                        filters.fromInstant() == null
                                ? null
                                : OffsetDateTime.ofInstant(
                                        filters.fromInstant(),
                                        ZoneOffset.UTC
                                )
                )
                .addValue(
                        "toInstant",
                        filters.toExclusiveInstant() == null
                                ? null
                                : OffsetDateTime.ofInstant(
                                        filters.toExclusiveInstant(),
                                        ZoneOffset.UTC
                                )
                )
                .addValue("scope", filters.scope().name());
    }

    public record Totals(
            long totalReceivedCents,
            long operationCount,
            long totalUnits
    ) {
    }

    private static final class OperationRowMapper
            implements RowMapper<SalesReportOperationResponse> {

        @Override
        public SalesReportOperationResponse mapRow(
                ResultSet resultSet,
                int rowNumber
        ) throws SQLException {
            return new SalesReportOperationResponse(
                    resultSet.getLong("operation_id"),
                    resultSet.getString("display_name"),
                    resultSet.getObject("completed_at", OffsetDateTime.class),
                    resultSet.getString("payment_method"),
                    resultSet.getString("responsible_user_name"),
                    resultSet.getLong("gross_cents"),
                    resultSet.getLong("allocated_discount_cents"),
                    resultSet.getLong("net_cents"),
                    resultSet.getInt("line_count"),
                    resultSet.getInt("total_units")
            );
        }
    }
}
