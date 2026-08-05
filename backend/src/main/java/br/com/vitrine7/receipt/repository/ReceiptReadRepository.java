package br.com.vitrine7.receipt.repository;

import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.receipt.dto.ReceiptCustomerResponse;
import br.com.vitrine7.receipt.dto.ReceiptEstablishmentResponse;
import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptPaymentResponse;
import br.com.vitrine7.receipt.dto.ReceiptVehicleResponse;
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
                                    cs.id,
                                    cs.operation_type,
                                    cs.source_id,
                                    cs.status,
                                    cs.subtotal_cents,
                                    cs.discount_cents,
                                    cs.total_cents,
                                    cs.created_by_user_id,
                                    creator.name AS created_by_user_name
                                FROM checkout_sessions cs
                                LEFT JOIN users creator
                                    ON creator.id = cs.created_by_user_id
                                WHERE cs.id = :checkoutId
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
                              'REVERSAL_PENDING',
                              'REVERSED'
                          )
                        """,
                params(checkoutId),
                Number.class
        );

        return count == null ? 0L : count.longValue();
    }

    public Optional<ReceiptPaymentResponse> findReceiptPayment(
            UUID checkoutId,
            String timeZone
    ) {
        return jdbcTemplate.query(
                        """
                                SELECT
                                    p.id,
                                    CASE p.method
                                        WHEN 'CASH' THEN 'CASH'
                                        WHEN 'PIX' THEN 'PIX'
                                        WHEN 'CREDIT_CARD' THEN 'CREDIT'
                                        WHEN 'DEBIT_CARD' THEN 'DEBIT'
                                    END AS method,
                                    p.processing_mode,
                                    p.status,
                                    p.amount_cents,
                                    p.approved_at,
                                    p.cash_received_cents,
                                    p.cash_change_cents,
                                    terminal.provider,
                                    p.reversed_at,
                                    p.reversal_reason
                                FROM payments p
                                LEFT JOIN payment_terminal_transactions terminal
                                    ON terminal.payment_id = p.id
                                   AND terminal.status = 'APPROVED'
                                WHERE p.checkout_session_id = :checkoutId
                                  AND p.status IN (
                                      'APPROVED',
                                      'REVERSAL_PENDING',
                                      'REVERSED'
                                  )
                                ORDER BY p.approved_at ASC, p.id ASC
                                """,
                        params(checkoutId).addValue("timeZone", timeZone),
                        (rs, rowNum) -> new ReceiptPaymentResponse(
                                rs.getObject("id", UUID.class),
                                rs.getString("method"),
                                rs.getString("processing_mode"),
                                rs.getString("status"),
                                rs.getLong("amount_cents"),
                                rs.getObject(
                                        "approved_at",
                                        OffsetDateTime.class
                                ),
                                nullableLong(
                                        rs,
                                        "cash_received_cents"
                                ),
                                nullableLong(
                                        rs,
                                        "cash_change_cents"
                                ),
                                rs.getString("provider"),
                                rs.getObject(
                                        "reversed_at",
                                        OffsetDateTime.class
                                ),
                                rs.getString("reversal_reason")
                        )
                )
                .stream()
                .findFirst();
    }

    public ReceiptEstablishmentResponse findEstablishment() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT company_name, cnpj, phone, address
                        FROM system_settings
                        WHERE id = 1
                        """,
                new MapSqlParameterSource(),
                (rs, rowNum) -> new ReceiptEstablishmentResponse(
                        blankToNull(rs.getString("company_name")),
                        blankToNull(rs.getString("cnpj")),
                        blankToNull(rs.getString("phone")),
                        blankToNull(rs.getString("address"))
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
                                    tab.total_cents
                                FROM bar_tabs tab
                                WHERE tab.checkout_session_id = :checkoutId
                                """,
                        params(checkoutId),
                        (rs, rowNum) -> new TabReceiptRow(
                                rs.getLong("id"),
                                rs.getObject(
                                        "checkout_session_id",
                                        UUID.class
                                ),
                                rs.getString("name"),
                                rs.getString("status"),
                                rs.getLong("subtotal_cents"),
                                rs.getLong("discount_cents"),
                                rs.getLong("total_cents")
                        )
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "BAR_TAB_NOT_FOUND",
                        "Comanda nao encontrada."
                ));
    }

    public WorkOrderReceiptRow findWorkOrder(UUID checkoutId) {
        return jdbcTemplate.query(
                        """
                                SELECT
                                    wo.id,
                                    wo.checkout_session_id,
                                    wo.customer_name_snapshot,
                                    wo.customer_phone_digits_snapshot,
                                    wo.vehicle_name_snapshot,
                                    wo.vehicle_plate_snapshot,
                                    wo.vehicle_size,
                                    wo.status,
                                    wo.subtotal_cents,
                                    wo.discount_cents,
                                    wo.total_cents
                                FROM lava_work_orders wo
                                WHERE wo.checkout_session_id = :checkoutId
                                """,
                        params(checkoutId),
                        (rs, rowNum) -> new WorkOrderReceiptRow(
                                rs.getLong("id"),
                                rs.getObject(
                                        "checkout_session_id",
                                        UUID.class
                                ),
                                rs.getString("customer_name_snapshot"),
                                rs.getString(
                                        "customer_phone_digits_snapshot"
                                ),
                                rs.getString("vehicle_name_snapshot"),
                                rs.getString("vehicle_plate_snapshot"),
                                rs.getString("vehicle_size"),
                                rs.getString("status"),
                                rs.getLong("subtotal_cents"),
                                rs.getLong("discount_cents"),
                                rs.getLong("total_cents")
                        )
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "LAVA_WORK_ORDER_NOT_FOUND",
                        "Ordem de servico nao encontrada."
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

    public List<ReceiptLineResponse> findWorkOrderLines(long workOrderId) {
        return jdbcTemplate.query(
                """
                        SELECT
                            service_name_snapshot,
                            price_cents
                        FROM lava_work_order_lines
                        WHERE work_order_id = :operationId
                        ORDER BY id ASC
                        """,
                params(workOrderId),
                (rs, rowNum) -> new ReceiptLineResponse(
                        rs.getString("service_name_snapshot"),
                        null,
                        1,
                        rs.getLong("price_cents"),
                        rs.getLong("price_cents")
                )
        );
    }

    private MapSqlParameterSource params(UUID checkoutId) {
        return new MapSqlParameterSource()
                .addValue("checkoutId", checkoutId);
    }

    private MapSqlParameterSource params(long operationId) {
        return new MapSqlParameterSource()
                .addValue("operationId", operationId);
    }

    private static Long nullableLong(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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
            long totalCents
    ) {
    }

    public record WorkOrderReceiptRow(
            long id,
            UUID checkoutId,
            String customerName,
            String customerPhone,
            String vehicleName,
            String vehiclePlate,
            String vehicleSize,
            String status,
            long subtotalCents,
            long discountCents,
            long totalCents
    ) {

        public ReceiptCustomerResponse customer() {
            return new ReceiptCustomerResponse(
                    customerName,
                    customerPhone
            );
        }

        public ReceiptVehicleResponse vehicle() {
            if (vehicleName == null
                    && vehiclePlate == null
                    && vehicleSize == null) {
                return null;
            }

            return new ReceiptVehicleResponse(
                    vehicleName,
                    vehiclePlate,
                    vehicleSize
            );
        }
    }

    private static final class CheckoutReceiptRowMapper
            implements RowMapper<CheckoutReceiptRow> {

        @Override
        public CheckoutReceiptRow mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return new CheckoutReceiptRow(
                    rs.getObject("id", UUID.class),
                    rs.getString("operation_type"),
                    nullableLong(rs, "source_id"),
                    rs.getString("status"),
                    rs.getLong("subtotal_cents"),
                    rs.getLong("discount_cents"),
                    rs.getLong("total_cents"),
                    nullableLong(rs, "created_by_user_id"),
                    rs.getString("created_by_user_name")
            );
        }
    }

    private static final class BarLineMapper
            implements RowMapper<ReceiptLineResponse> {

        @Override
        public ReceiptLineResponse mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return new ReceiptLineResponse(
                    rs.getString("item_name_snapshot"),
                    rs.getString("category_name_snapshot"),
                    rs.getInt("quantity"),
                    rs.getLong("unit_price_cents"),
                    rs.getLong("line_total_cents")
            );
        }
    }
}
