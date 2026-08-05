package br.com.vitrine7.history.lava.repository;

import br.com.vitrine7.history.bar.dto.HistoryPaymentMethod;
import br.com.vitrine7.history.lava.dto.LavaHistoryFilters;
import br.com.vitrine7.history.lava.dto.LavaHistoryResponse;
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
public class LavaHistoryReadRepository {

    private static final String BASE_SQL = """
            WITH history AS (
                SELECT
                    wo.id AS work_order_id,
                    wo.checkout_session_id AS checkout_id,
                    wo.customer_name_snapshot,
                    wo.customer_phone_digits_snapshot,
                    wo.vehicle_name_snapshot,
                    wo.vehicle_plate_snapshot,
                    wo.vehicle_size,
                    wo.status,
                    wo.subtotal_cents,
                    wo.discount_cents,
                    wo.total_cents,
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
                    COALESCE(lines.service_count, 0)::integer AS service_count,
                    wo.created_at,
                    wo.paid_at,
                    wo.completed_at,
                    wo.cancelled_at,
                    wo.created_by_user_id,
                    COALESCE(
                        wo.completed_at,
                        wo.paid_at,
                        wo.cancelled_at,
                        cs.finalized_at,
                        wo.updated_at,
                        wo.created_at
                    ) AS event_at,
                    LOWER(
                        wo.normalized_customer_name_snapshot || ' ' ||
                        COALESCE(wo.customer_phone_digits_snapshot, '') || ' ' ||
                        COALESCE(wo.normalized_vehicle_name_snapshot, '') || ' ' ||
                        COALESCE(wo.vehicle_plate_snapshot, '') || ' ' ||
                        wo.id || ' ' ||
                        COALESCE(lines.search_text, '')
                    ) AS search_text
                FROM lava_work_orders wo
                LEFT JOIN checkout_sessions cs
                    ON cs.id = wo.checkout_session_id
                LEFT JOIN LATERAL (
                    SELECT
                        pay.id,
                        pay.method,
                        pay.status,
                        pay.reversed_at,
                        pay.reversal_reason
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
                        COUNT(*) AS service_count,
                        STRING_AGG(line.normalized_service_name_snapshot, ' ') AS search_text
                    FROM lava_work_order_lines line
                    WHERE line.work_order_id = wo.id
                ) lines ON TRUE
                WHERE wo.status = 'COMPLETED'
                  AND p.id IS NOT NULL
            )
            SELECT *
            FROM history
            WHERE (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
              AND (CAST(:paymentMethod AS text) IS NULL OR payment_method = CAST(:paymentMethod AS text))
              AND (CAST(:fromDate AS timestamptz) IS NULL OR event_at >= CAST(:fromDate AS timestamptz))
              AND (CAST(:toDate AS timestamptz) IS NULL OR event_at <= CAST(:toDate AS timestamptz))
              AND (CAST(:search AS text) IS NULL OR search_text LIKE CAST(:search AS text))
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<LavaHistoryResponse> findRecent(int limit) {
        MapSqlParameterSource params = baseParams();
        params.addValue("limit", limit);

        return jdbcTemplate.query(
                BASE_SQL + """
                        ORDER BY event_at DESC, work_order_id DESC
                        LIMIT :limit
                        """,
                params,
                new LavaHistoryRowMapper()
        );
    }

    public List<LavaHistoryResponse> findPage(LavaHistoryFilters filters) {
        MapSqlParameterSource params = params(filters);
        params.addValue("limit", filters.size());
        params.addValue("offset", filters.page() * filters.size());

        return jdbcTemplate.query(
                BASE_SQL + """
                        ORDER BY event_at DESC, work_order_id DESC
                        LIMIT :limit
                        OFFSET :offset
                        """,
                params,
                new LavaHistoryRowMapper()
        );
    }

    public long count(LavaHistoryFilters filters) {
        Number count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (" + BASE_SQL + ") counted",
                params(filters),
                Number.class
        );

        return count == null ? 0L : count.longValue();
    }

    private MapSqlParameterSource params(LavaHistoryFilters filters) {
        return baseParams()
                .addValue(
                        "status",
                        filters.status() == null
                                ? null
                                : filters.status().name()
                )
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
                .addValue("status", null)
                .addValue("paymentMethod", null)
                .addValue("fromDate", null)
                .addValue("toDate", null)
                .addValue("search", null);
    }

    private static final class LavaHistoryRowMapper
            implements RowMapper<LavaHistoryResponse> {

        @Override
        public LavaHistoryResponse mapRow(
                ResultSet rs,
                int rowNum
        ) throws SQLException {
            Long workOrderId = rs.getLong("work_order_id");

            return new LavaHistoryResponse(
                    workOrderId,
                    rs.getObject("checkout_id", UUID.class),
                    rs.getString("customer_name_snapshot"),
                    rs.getString("customer_phone_digits_snapshot"),
                    rs.getString("vehicle_name_snapshot"),
                    rs.getString("vehicle_plate_snapshot"),
                    rs.getString("vehicle_size"),
                    rs.getString("status"),
                    rs.getLong("subtotal_cents"),
                    rs.getLong("discount_cents"),
                    rs.getLong("total_cents"),
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
                    rs.getInt("service_count"),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("paid_at", OffsetDateTime.class),
                    rs.getObject("completed_at", OffsetDateTime.class),
                    rs.getObject("cancelled_at", OffsetDateTime.class),
                    rs.getLong("created_by_user_id"),
                    "/api/v1/lava/work-orders/" + workOrderId
            );
        }

        private HistoryPaymentMethod paymentMethod(String value) {
            return value == null
                    ? null
                    : HistoryPaymentMethod.valueOf(value);
        }
    }
}
