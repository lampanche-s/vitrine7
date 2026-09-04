package br.com.vitrine7.receipt.repository;

import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReceiptReadRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CheckoutReceiptRow findCheckout(UUID checkoutId) {
        return jdbcTemplate.query(
                        """
                                SELECT
                                    checkout.id,
                                    checkout.operation_type,
                                    checkout.source_id,
                                    checkout.status,
                                    checkout.subtotal_cents,
                                    checkout.discount_cents,
                                    checkout.total_cents,
                                    checkout.created_by_user_id,
                                    creator.name AS created_by_user_name
                                FROM checkout_sessions checkout
                                LEFT JOIN users creator
                                    ON creator.id = checkout.created_by_user_id
                                WHERE checkout.id = :checkoutId
                                """,
                        params(checkoutId),
                        new CheckoutReceiptRowMapper()
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "CHECKOUT_NOT_FOUND",
                        "Checkout nao encontrado."
                ));
    }

    public long countReceiptPayments(UUID checkoutId) {
        Number count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM payments
                        WHERE checkout_session_id = :checkoutId
                          AND status IN (
                              'APPROVED',
                              'REVERSED'
                          )
                        """,
                params(checkoutId),
                Number.class
        );
        return count == null ? 0L : count.longValue();
    }

    public List<ReceiptPaymentResponse> findReceiptPayments(UUID checkoutId) {
        return jdbcTemplate.query(
                        """
                                SELECT
                                    payment.id,
                                    CASE payment.method
                                        WHEN 'CASH' THEN 'CASH'
                                        WHEN 'PIX' THEN 'PIX'
                                        WHEN 'CREDIT_CARD' THEN 'CREDIT'
                                        WHEN 'DEBIT_CARD' THEN 'DEBIT'
                                    END AS method,
                                    payment.processing_mode,
                                    payment.status,
                                    payment.amount_cents,
                                    payment.approved_at,
                                    payment.cash_received_cents,
                                    payment.cash_change_cents,
                                    terminal.provider,
                                    payment.reversed_at,
                                    payment.reversal_reason
                                FROM payments payment
                                LEFT JOIN payment_terminal_transactions terminal
                                    ON terminal.payment_id = payment.id
                                   AND terminal.status = 'APPROVED'
                                WHERE payment.checkout_session_id = :checkoutId
                                  AND payment.status IN (
                                      'APPROVED',
                                      'REVERSED'
                                  )
                                ORDER BY payment.approved_at ASC, payment.id ASC
                                """,
                        params(checkoutId),
                        (resultSet, rowNumber) -> new ReceiptPaymentResponse(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("method"),
                                resultSet.getString("processing_mode"),
                                resultSet.getString("status"),
                                resultSet.getLong("amount_cents"),
                                resultSet.getObject("approved_at", OffsetDateTime.class),
                                nullableLong(resultSet, "cash_received_cents"),
                                nullableLong(resultSet, "cash_change_cents"),
                                resultSet.getString("provider"),
                                resultSet.getObject("reversed_at", OffsetDateTime.class),
                                resultSet.getString("reversal_reason")
                        )
                );
    }

    public TabReceiptRow findTab(UUID checkoutId) {
        return jdbcTemplate.query(
                        """
                                SELECT
                                    tab.id,
                                    tab.checkout_session_id,
                                    tab.name,
                                    tab.status,
                                    tab.subtotal_cents,
                                    tab.discount_cents,
                                    tab.total_cents,
                                    tab.vehicle_name_snapshot,
                                    tab.vehicle_plate_snapshot
                                FROM bar_tabs tab
                                WHERE tab.checkout_session_id = :checkoutId
                                """,
                        params(checkoutId),
                        (resultSet, rowNumber) -> new TabReceiptRow(
                                resultSet.getLong("id"),
                                resultSet.getObject("checkout_session_id", UUID.class),
                                resultSet.getString("name"),
                                resultSet.getString("status"),
                                resultSet.getLong("subtotal_cents"),
                                resultSet.getLong("discount_cents"),
                                resultSet.getLong("total_cents"),
                                resultSet.getString("vehicle_name_snapshot"),
                                resultSet.getString("vehicle_plate_snapshot")
                        )
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "BAR_TAB_NOT_FOUND",
                        "Comanda nao encontrada."
                ));
    }

    public List<ReceiptLineResponse> findTabLines(long tabId) {
        return jdbcTemplate.query(
                """
                        SELECT
                            item_name_snapshot,
                            entry_type_snapshot AS category_name_snapshot,
                            quantity,
                            unit_price_cents,
                            line_total_cents
                        FROM bar_tab_lines
                        WHERE tab_id = :operationId
                        ORDER BY id ASC
                        """,
                params(tabId),
                new BarLineMapper()
        );
    }

    private MapSqlParameterSource params(UUID checkoutId) {
        return new MapSqlParameterSource().addValue("checkoutId", checkoutId);
    }

    private MapSqlParameterSource params(long operationId) {
        return new MapSqlParameterSource().addValue("operationId", operationId);
    }

    private static Long nullableLong(ResultSet resultSet, String column)
            throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    public record CheckoutReceiptRow(
            UUID id,
            String operationType,
            Long sourceId,
            String status,
            long subtotalCents,
            long discountCents,
            long totalCents,
            Long createdByUserId,
            String createdByUserName
    ) {
    }

    public record TabReceiptRow(
            long id,
            UUID checkoutId,
            String name,
            String status,
            long subtotalCents,
            long discountCents,
            long totalCents,
            String vehicleName,
            String vehiclePlate
    ) {
    }

    private static final class CheckoutReceiptRowMapper
            implements RowMapper<CheckoutReceiptRow> {

        @Override
        public CheckoutReceiptRow mapRow(ResultSet resultSet, int rowNumber)
                throws SQLException {
            return new CheckoutReceiptRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("operation_type"),
                    nullableLong(resultSet, "source_id"),
                    resultSet.getString("status"),
                    resultSet.getLong("subtotal_cents"),
                    resultSet.getLong("discount_cents"),
                    resultSet.getLong("total_cents"),
                    nullableLong(resultSet, "created_by_user_id"),
                    resultSet.getString("created_by_user_name")
            );
        }
    }

    private static final class BarLineMapper
            implements RowMapper<ReceiptLineResponse> {

        @Override
        public ReceiptLineResponse mapRow(ResultSet resultSet, int rowNumber)
                throws SQLException {
            return new ReceiptLineResponse(
                    resultSet.getString("item_name_snapshot"),
                    resultSet.getString("category_name_snapshot"),
                    resultSet.getInt("quantity"),
                    resultSet.getLong("unit_price_cents"),
                    resultSet.getLong("line_total_cents")
            );
        }
    }
}
