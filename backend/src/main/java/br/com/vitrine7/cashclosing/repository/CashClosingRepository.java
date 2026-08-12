package br.com.vitrine7.cashclosing.repository;

import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CashClosingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CashClosingRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CashClosingResponse.Operation> operations(
            long userId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return jdbcTemplate.query(
                """
                        SELECT
                            tab.id AS operation_id,
                            tab.name AS display_name,
                            COALESCE(
                                tab.closed_at,
                                checkout.finalized_at,
                                payment.approved_at,
                                tab.updated_at
                            ) AS completed_at,
                            payment.method AS payment_method,
                            payment.status AS payment_status,
                            payment.amount_cents,
                            COALESCE(payment.cash_received_cents, 0) AS cash_received_cents,
                            COALESCE(payment.cash_change_cents, 0) AS cash_change_cents
                        FROM payments payment
                        JOIN checkout_sessions checkout
                          ON checkout.id = payment.checkout_session_id
                         AND checkout.operation_type = 'BAR_COMMAND'
                        JOIN bar_tabs tab
                          ON tab.checkout_session_id = checkout.id
                        WHERE tab.status = 'CLOSED'
                          AND checkout.status = 'FINALIZED'
                          AND payment.status IN ('APPROVED', 'REVERSED')
                          AND COALESCE(
                                payment.approved_by_user_id,
                                payment.created_by_user_id
                              ) = :userId
                          AND COALESCE(
                                tab.closed_at,
                                checkout.finalized_at,
                                payment.approved_at,
                                tab.updated_at
                              ) >= :fromInstant
                          AND COALESCE(
                                tab.closed_at,
                                checkout.finalized_at,
                                payment.approved_at,
                                tab.updated_at
                              ) < :toInstant
                        ORDER BY completed_at ASC, operation_id ASC
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("fromInstant", from)
                        .addValue("toInstant", to),
                (resultSet, rowNumber) -> new CashClosingResponse.Operation(
                        resultSet.getLong("operation_id"),
                        resultSet.getString("display_name"),
                        resultSet.getObject("completed_at", OffsetDateTime.class),
                        resultSet.getString("payment_method"),
                        resultSet.getString("payment_status"),
                        resultSet.getLong("amount_cents"),
                        resultSet.getLong("cash_received_cents"),
                        resultSet.getLong("cash_change_cents")
                )
        );
    }


    public OpenCommandsSummary openCommands(
            long userId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT
                            COUNT(*) AS command_count,
                            COALESCE(SUM(total_cents), 0) AS amount_cents
                        FROM bar_tabs
                        WHERE created_by_user_id = :userId
                          AND status IN ('OPEN', 'PAYMENT_PENDING')
                          AND created_at >= :fromInstant
                          AND created_at < :toInstant
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("fromInstant", from)
                        .addValue("toInstant", to),
                (resultSet, rowNumber) -> new OpenCommandsSummary(
                        resultSet.getLong("command_count"),
                        resultSet.getLong("amount_cents")
                )
        );
    }

    public Optional<OffsetDateTime> findClosedAt(
            long userId,
            LocalDate businessDate
    ) {
        return jdbcTemplate.query(
                """
                        SELECT closed_at
                        FROM cash_closures
                        WHERE user_id = :userId
                          AND business_date = :businessDate
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("businessDate", businessDate),
                (resultSet, rowNumber) ->
                        resultSet.getObject("closed_at", OffsetDateTime.class)
        ).stream().findFirst();
    }

    public void upsert(
            long userId,
            LocalDate businessDate,
            CashClosingResponse summary,
            OffsetDateTime closedAt
    ) {
        long cash = amountFor(summary, "CASH");
        long pix = amountFor(summary, "PIX");
        long credit = amountFor(summary, "CREDIT_CARD");
        long debit = amountFor(summary, "DEBIT_CARD");

        jdbcTemplate.update(
                """
                        INSERT INTO cash_closures (
                            user_id,
                            business_date,
                            total_received_cents,
                            sale_count,
                            reversed_cents,
                            reversed_count,
                            cash_cents,
                            pix_cents,
                            credit_cents,
                            debit_cents,
                            first_sale_at,
                            last_sale_at,
                            closed_at,
                            updated_at
                        ) VALUES (
                            :userId,
                            :businessDate,
                            :totalReceivedCents,
                            :saleCount,
                            :reversedCents,
                            :reversedCount,
                            :cashCents,
                            :pixCents,
                            :creditCents,
                            :debitCents,
                            :firstSaleAt,
                            :lastSaleAt,
                            :closedAt,
                            :closedAt
                        )
                        ON CONFLICT (user_id, business_date)
                        DO UPDATE SET
                            total_received_cents = EXCLUDED.total_received_cents,
                            sale_count = EXCLUDED.sale_count,
                            reversed_cents = EXCLUDED.reversed_cents,
                            reversed_count = EXCLUDED.reversed_count,
                            cash_cents = EXCLUDED.cash_cents,
                            pix_cents = EXCLUDED.pix_cents,
                            credit_cents = EXCLUDED.credit_cents,
                            debit_cents = EXCLUDED.debit_cents,
                            first_sale_at = EXCLUDED.first_sale_at,
                            last_sale_at = EXCLUDED.last_sale_at,
                            closed_at = EXCLUDED.closed_at,
                            updated_at = EXCLUDED.updated_at
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("businessDate", businessDate)
                        .addValue("totalReceivedCents", summary.totalReceivedCents())
                        .addValue("saleCount", summary.saleCount())
                        .addValue("reversedCents", summary.reversedCents())
                        .addValue("reversedCount", summary.reversedCount())
                        .addValue("cashCents", cash)
                        .addValue("pixCents", pix)
                        .addValue("creditCents", credit)
                        .addValue("debitCents", debit)
                        .addValue("firstSaleAt", summary.firstSaleAt())
                        .addValue("lastSaleAt", summary.lastSaleAt())
                        .addValue("closedAt", closedAt)
        );
    }

    private long amountFor(CashClosingResponse summary, String method) {
        return summary.paymentBreakdown().stream()
                .filter(entry -> method.equals(entry.method()))
                .mapToLong(CashClosingResponse.PaymentBreakdown::amountCents)
                .sum();
    }
    public record OpenCommandsSummary(
            long count,
            long amountCents
    ) {
    }

}
