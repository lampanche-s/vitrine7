package br.com.vitrine7.history.bar.repository;

import br.com.vitrine7.history.bar.dto.BarHistoryFilters;
import br.com.vitrine7.history.bar.dto.BarHistoryResponse;
import br.com.vitrine7.history.bar.dto.HistoryPaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BarHistoryReadRepository {

    private static final String BASE_SQL = """
            WITH history AS (
                SELECT
                    tab.id AS operation_id,
                    tab.checkout_session_id AS checkout_id,
                    tab.name AS display_name,
                    tab.status AS operational_status,
                    cs.status AS checkout_status,
                    tab.subtotal_cents,
                    tab.discount_cents,
                    tab.total_cents,
                    cs.document_type,
                    CASE p.method
                        WHEN 'CASH' THEN 'CASH'
                        WHEN 'PIX' THEN 'PIX'
                        WHEN 'CREDIT_CARD' THEN 'CREDIT'
                        WHEN 'DEBIT_CARD' THEN 'DEBIT'
                        ELSE NULL
                    END AS payment_method,
                    p.status AS payment_status,
                    p.id AS payment_id,
                    p.reversed_at AS payment_reversed_at,
                    p.reversal_reason AS payment_reversal_reason,
                    p.cash_received_cents,
                    p.cash_change_cents,
                    COALESCE(lines.line_count, 0)::integer AS line_count,
                    COALESCE(lines.total_units, 0)::integer AS total_units,
                    tab.created_at,
                    COALESCE(
                        tab.closed_at,
                        cs.finalized_at,
                        tab.updated_at,
                        tab.created_at
                    ) AS finished_at,
                    tab.created_by_user_id,
                    ('/api/v1/bar/tabs/' || tab.id) AS detail_path,
                    LOWER(
                        tab.name || ' ' || tab.id || ' ' ||
                        COALESCE(lines.search_text, '')
                    ) AS search_text
                FROM bar_tabs tab
                JOIN checkout_sessions cs
                    ON cs.id = tab.checkout_session_id
                LEFT JOIN LATERAL (
                    SELECT
                        pay.id,
                        pay.method,
                        pay.status,
                        pay.reversed_at,
                        pay.reversal_reason,
                        pay.cash_received_cents,
                        pay.cash_change_cents
                    FROM payments pay
                    WHERE pay.checkout_session_id = cs.id
                      AND pay.status IN (
                          'APPROVED',
                          'REVERSAL_PENDING',
                          'REVERSED'
                      )
                    ORDER BY pay.approved_at DESC, pay.created_at DESC
                    LIMIT 1
                ) p ON TRUE
                LEFT JOIN LATERAL (
                    SELECT
                        COUNT(*) AS line_count,
                        COALESCE(SUM(line.quantity), 0) AS total_units,
                        STRING_AGG(line.item_name_snapshot, ' ') AS search_text
                    FROM bar_tab_lines line
                    WHERE line.tab_id = tab.id
                ) lines ON TRUE
                WHERE tab.status = 'CLOSED'
                  AND cs.status = 'FINALIZED'
                  AND p.id IS NOT NULL
            )
            SELECT *
            FROM history
            WHERE (CAST(:paymentMethod AS text) IS NULL OR payment_method = CAST(:paymentMethod AS text))
              AND (CAST(:fromDate AS timestamptz) IS NULL OR finished_at >= CAST(:fromDate AS timestamptz))
              AND (CAST(:toDate AS timestamptz) IS NULL OR finished_at <= CAST(:toDate AS timestamptz))
              AND (CAST(:search AS text) IS NULL OR search_text LIKE CAST(:search AS text))
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<BarHistoryResponse> findRecent(int limit) {
        MapSqlParameterSource params = baseParams();
        params.addValue("limit", limit);

        return jdbcTemplate.query(
                BASE_SQL + """
                        ORDER BY finished_at DESC, operation_id DESC
                        LIMIT :limit
                        """,
                params,
                new BarHistoryRowMapper()
        );
    }

    public List<BarHistoryResponse> findPage(BarHistoryFilters filters) {
        MapSqlParameterSource params = params(filters);
        params.addValue("limit", filters.size());
        params.addValue("offset", filters.page() * filters.size());

        return jdbcTemplate.query(
                BASE_SQL + """
                        ORDER BY finished_at DESC, operation_id DESC
                        LIMIT :limit
                        OFFSET :offset
                        """,
                params,
                new BarHistoryRowMapper()
        );
    }

    public long count(BarHistoryFilters filters) {
        Number count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (" + BASE_SQL + ") counted",
                params(filters),
                Number.class
        );

        return count == null ? 0L : count.longValue();
    }

    private MapSqlParameterSource params(BarHistoryFilters filters) {
        return baseParams()
                .addValue(
                        "paymentMethod",
                        filters.paymentMethod() == null
                                ? null
                                : filters.paymentMethod().name()
                )
                .addValue("fromDate", filters.from())
                .addValue("toDate", filters.to())
                .addValue(
                        "search",
                        filters.normalizedSearch() == null
                                ? null
                                : "%" + filters.normalizedSearch() + "%"
                );
    }

    private MapSqlParameterSource baseParams() {
        return new MapSqlParameterSource()
                .addValue("paymentMethod", null)
                .addValue("fromDate", null)
                .addValue("toDate", null)
                .addValue("search", null);
    }

    private static final class BarHistoryRowMapper
            implements RowMapper<BarHistoryResponse> {

        @Override
        public BarHistoryResponse mapRow(
                ResultSet rs,
                int rowNum
        ) throws SQLException {
            return new BarHistoryResponse(
                    rs.getLong("operation_id"),
                    rs.getObject("checkout_id", UUID.class),
                    rs.getString("display_name"),
                    rs.getString("operational_status"),
                    rs.getString("checkout_status"),
                    rs.getLong("subtotal_cents"),
                    rs.getLong("discount_cents"),
                    rs.getLong("total_cents"),
                    rs.getString("document_type"),
                    paymentMethod(rs.getString("payment_method")),
                    rs.getString("payment_status"),
                    rs.getObject("payment_id", UUID.class),
                    rs.getObject(
                            "payment_reversed_at",
                            OffsetDateTime.class
                    ),
                    rs.getString(
                            "payment_reversal_reason"
                    ),
                    nullableLong(rs, "cash_received_cents"),
                    nullableLong(rs, "cash_change_cents"),
                    rs.getInt("line_count"),
                    rs.getInt("total_units"),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("finished_at", OffsetDateTime.class),
                    rs.getLong("created_by_user_id"),
                    rs.getString("detail_path")
            );
        }

        private HistoryPaymentMethod paymentMethod(String value) {
            return value == null
                    ? null
                    : HistoryPaymentMethod.valueOf(value);
        }

        private Long nullableLong(ResultSet rs, String column)
                throws SQLException {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        }
    }
}
