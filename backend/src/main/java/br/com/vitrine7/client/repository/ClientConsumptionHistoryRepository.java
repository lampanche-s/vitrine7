package br.com.vitrine7.client.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ClientConsumptionHistoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClientConsumptionHistoryRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Row> findByClientId(long clientId) {
        return jdbcTemplate.query(
                """
                        WITH paid_tabs AS (
                            SELECT
                                tab.id,
                                COALESCE(tab.closed_at, checkout.finalized_at) AS completed_at,
                                tab.total_cents,
                                CASE
                                    WHEN BOOL_AND(payment.status = 'REVERSED') THEN 'REVERSED'
                                    ELSE 'APPROVED'
                                END AS payment_status
                            FROM bar_tabs tab
                            JOIN checkout_sessions checkout
                              ON checkout.id = tab.checkout_session_id
                             AND checkout.status = 'FINALIZED'
                            JOIN payments payment
                              ON payment.checkout_session_id = checkout.id
                             AND payment.status IN ('APPROVED', 'REVERSED')
                            WHERE tab.client_id = :clientId
                              AND tab.status = 'CLOSED'
                            GROUP BY tab.id, tab.closed_at, checkout.finalized_at, tab.total_cents
                        )
                        SELECT
                            paid.id AS operation_id,
                            paid.completed_at,
                            paid.total_cents,
                            paid.payment_status,
                            line.entry_type_snapshot AS entry_type,
                            line.item_name_snapshot AS item_name,
                            line.quantity,
                            line.unit_price_cents,
                            line.line_total_cents
                        FROM paid_tabs paid
                        JOIN bar_tab_lines line
                          ON line.tab_id = paid.id
                        ORDER BY paid.completed_at DESC, paid.id DESC, line.id ASC
                        """,
                new MapSqlParameterSource("clientId", clientId),
                (rs, rowNum) -> new Row(
                        rs.getLong("operation_id"),
                        rs.getObject("completed_at", OffsetDateTime.class),
                        rs.getLong("total_cents"),
                        rs.getString("payment_status"),
                        rs.getString("entry_type"),
                        rs.getString("item_name"),
                        rs.getInt("quantity"),
                        rs.getLong("unit_price_cents"),
                        rs.getLong("line_total_cents")
                )
        );
    }

    public record Row(
            long operationId,
            OffsetDateTime completedAt,
            long totalCents,
            String paymentStatus,
            String entryType,
            String itemName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents
    ) {
    }
}
