package br.com.vitrine7.finance.repository;

import br.com.vitrine7.finance.dto.FinanceBreakdownResponse;
import br.com.vitrine7.finance.dto.FinanceDailyResponse;
import br.com.vitrine7.finance.dto.FinanceFilters;
import br.com.vitrine7.finance.dto.FinanceModule;
import br.com.vitrine7.finance.dto.FinancePaymentMethod;
import br.com.vitrine7.finance.dto.FinancePeriodResponse;
import br.com.vitrine7.finance.dto.FinanceSummaryResponse;
import br.com.vitrine7.finance.dto.FinanceTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FinanceReadRepository {

    private static final String BASE_SQL = """
            WITH financial_payments AS (
                SELECT
                    p.id AS payment_id,
                    p.checkout_session_id AS checkout_id,
                    cs.operation_type,
                    CASE
                        WHEN cs.operation_type = 'BAR_COMMAND' THEN 'BAR'
                        WHEN cs.operation_type = 'LAVA_WORK_ORDER' THEN 'LAVA'
                    END AS module,
                    CASE p.method
                        WHEN 'CASH' THEN 'CASH'
                        WHEN 'PIX' THEN 'PIX'
                        WHEN 'CREDIT_CARD' THEN 'CREDIT'
                        WHEN 'DEBIT_CARD' THEN 'DEBIT'
                    END AS payment_method,
                    p.method AS payment_method_database,
                    p.processing_mode,
                    p.status,
                    p.amount_cents,
                    p.approved_at,
                    COALESCE(p.approved_by_user_id, p.created_by_user_id)
                        AS responsible_user_id,
                    responsible.name AS responsible_user_name,
                    cs.source_id AS operation_id,
                    CASE cs.operation_type
                        WHEN 'BAR_COMMAND' THEN tab.name
                        WHEN 'LAVA_WORK_ORDER' THEN COALESCE(
                            NULLIF(BTRIM(wo.customer_name_snapshot), ''),
                            'Cliente avulso'
                        )
                    END AS display_name,
                    CASE cs.operation_type
                        WHEN 'BAR_COMMAND' THEN
                            COALESCE(tab_lines.line_count, 0)
                                || ' linhas da comanda'
                        WHEN 'LAVA_WORK_ORDER' THEN
                            BTRIM(CONCAT_WS(
                                ' | ',
                                NULLIF(BTRIM(CONCAT_WS(
                                    ' ',
                                    wo.vehicle_name_snapshot,
                                    wo.vehicle_plate_snapshot
                                )), ''),
                                COALESCE(work_order_lines.service_count, 0)
                                    || ' servicos'
                            ))
                    END AS description,
                    CASE cs.operation_type
                        WHEN 'BAR_COMMAND' THEN
                            '/api/v1/bar/tabs/' || cs.source_id
                        WHEN 'LAVA_WORK_ORDER' THEN
                            '/api/v1/lava/work-orders/' || cs.source_id
                    END AS detail_path,
                    (p.approved_at AT TIME ZONE CAST(:timeZone AS text))::date
                        AS financial_date,
                    LOWER(
                        p.id::text || ' ' ||
                        p.checkout_session_id::text || ' ' ||
                        COALESCE(tab.normalized_name, '') || ' ' ||
                        COALESCE(tab.name, '') || ' ' ||
                        COALESCE(wo.normalized_customer_name_snapshot, '') || ' ' ||
                        COALESCE(wo.customer_name_snapshot, '') || ' ' ||
                        COALESCE(wo.customer_phone_digits_snapshot, '') || ' ' ||
                        COALESCE(wo.normalized_vehicle_name_snapshot, '') || ' ' ||
                        COALESCE(wo.vehicle_name_snapshot, '') || ' ' ||
                        COALESCE(wo.vehicle_plate_snapshot, '')
                    ) AS search_text
                FROM payments p
                JOIN checkout_sessions cs
                    ON cs.id = p.checkout_session_id
                LEFT JOIN users responsible
                    ON responsible.id =
                        COALESCE(p.approved_by_user_id, p.created_by_user_id)
                LEFT JOIN bar_tabs tab
                    ON tab.checkout_session_id = cs.id
                   AND cs.operation_type = 'BAR_COMMAND'
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::integer AS line_count
                    FROM bar_tab_lines line
                    WHERE line.tab_id = tab.id
                ) tab_lines ON TRUE
                LEFT JOIN lava_work_orders wo
                    ON wo.checkout_session_id = cs.id
                   AND cs.operation_type = 'LAVA_WORK_ORDER'
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::integer AS service_count
                    FROM lava_work_order_lines line
                    WHERE line.work_order_id = wo.id
                ) work_order_lines ON TRUE
                WHERE p.status = 'APPROVED'
                  AND p.approved_at >= CAST(:fromInstant AS timestamptz)
                  AND p.approved_at < CAST(:toInstant AS timestamptz)
                  AND (
                      CAST(:module AS text) IS NULL
                      OR (
                          CASE
                              WHEN cs.operation_type = 'BAR_COMMAND'
                                  THEN 'BAR'
                              WHEN cs.operation_type = 'LAVA_WORK_ORDER'
                                  THEN 'LAVA'
                          END
                      ) = CAST(:module AS text)
                  )
                  AND (
                      CAST(:operationType AS text) IS NULL
                      OR cs.operation_type = CAST(:operationType AS text)
                  )
                  AND (
                      CAST(:paymentMethod AS text) IS NULL
                      OR p.method = CAST(:paymentMethod AS text)
                  )
            )
            SELECT *
            FROM financial_payments
            WHERE (
                CAST(:search AS text) IS NULL
                OR search_text LIKE CAST(:search AS text)
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FinanceSummaryResponse summary(FinanceFilters filters) {
        SummaryTotals totals = jdbcTemplate.queryForObject(
                """
                        SELECT
                            COALESCE(SUM(amount_cents), 0) AS total_revenue_cents,
                            COUNT(*) AS payment_count,
                            MIN(approved_at) AS first_approved_at,
                            MAX(approved_at) AS last_approved_at
                        FROM (
                        """ + BASE_SQL + """
                        ) fp
                        """,
                params(filters),
                new SummaryTotalsRowMapper()
        );

        if (totals == null) {
            totals = new SummaryTotals(
                    0L,
                    0L,
                    null,
                    null
            );
        }

        long totalRevenue = totals.totalRevenueCents();

        return new FinanceSummaryResponse(
                period(filters),
                totalRevenue,
                totals.paymentCount(),
                average(totalRevenue, totals.paymentCount()),
                totals.firstApprovedAt(),
                totals.lastApprovedAt(),
                breakdown(filters, "module", totalRevenue),
                breakdown(filters, "operation_type", totalRevenue),
                breakdown(filters, "payment_method", totalRevenue)
        );
    }

    public List<FinanceDailyResponse> daily(FinanceFilters filters) {
        Map<LocalDate, DailyAccumulator> days =
                initialDays(filters);

        jdbcTemplate.query(
                """
                        SELECT
                            financial_date,
                            COALESCE(SUM(amount_cents), 0) AS revenue_cents,
                            COUNT(*) AS payment_count
                        FROM (
                        """ + BASE_SQL + """
                        ) fp
                        GROUP BY financial_date
                        ORDER BY financial_date ASC
                        """,
                params(filters),
                rs -> {
                    DailyAccumulator day = days.get(
                            rs.getObject("financial_date", LocalDate.class)
                    );
                    if (day != null) {
                        day.totalRevenueCents =
                                rs.getLong("revenue_cents");
                        day.paymentCount =
                                rs.getLong("payment_count");
                    }
                }
        );

        dailyBreakdown(filters, "payment_method", days);
        dailyBreakdown(filters, "module", days);

        return days.values()
                .stream()
                .map(DailyAccumulator::toResponse)
                .toList();
    }

    public List<FinanceTransactionResponse> transactions(
            FinanceFilters filters,
            int page,
            int size
    ) {
        MapSqlParameterSource params = params(filters)
                .addValue("limit", size)
                .addValue("offset", page * size);

        return jdbcTemplate.query(
                BASE_SQL + """
                        ORDER BY approved_at DESC, payment_id DESC
                        LIMIT :limit
                        OFFSET :offset
                        """,
                params,
                new TransactionRowMapper()
        );
    }

    public long countTransactions(FinanceFilters filters) {
        Number count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (" + BASE_SQL + ") counted",
                params(filters),
                Number.class
        );

        return count == null ? 0L : count.longValue();
    }

    private List<FinanceBreakdownResponse> breakdown(
            FinanceFilters filters,
            String column,
            long totalRevenue
    ) {
        String sql = """
                SELECT
                    %s AS breakdown_key,
                    COALESCE(SUM(amount_cents), 0) AS revenue_cents,
                    COUNT(*) AS payment_count
                FROM (
                %s
                ) fp
                GROUP BY %s
                ORDER BY revenue_cents DESC, breakdown_key ASC
                """.formatted(column, BASE_SQL, column);

        return jdbcTemplate.query(
                sql,
                params(filters),
                (rs, rowNum) -> new FinanceBreakdownResponse(
                        rs.getString("breakdown_key"),
                        rs.getLong("revenue_cents"),
                        rs.getLong("payment_count"),
                        basisPoints(
                                rs.getLong("revenue_cents"),
                                totalRevenue
                        )
                )
        );
    }

    private void dailyBreakdown(
            FinanceFilters filters,
            String column,
            Map<LocalDate, DailyAccumulator> days
    ) {
        String sql = """
                SELECT
                    financial_date,
                    %s AS breakdown_key,
                    COALESCE(SUM(amount_cents), 0) AS revenue_cents,
                    COUNT(*) AS payment_count
                FROM (
                %s
                ) fp
                GROUP BY financial_date, %s
                ORDER BY financial_date ASC, breakdown_key ASC
                """.formatted(column, BASE_SQL, column);

        jdbcTemplate.query(
                sql,
                params(filters),
                rs -> {
                    DailyAccumulator day = days.get(
                            rs.getObject("financial_date", LocalDate.class)
                    );
                    if (day == null) {
                        return;
                    }

                    FinanceBreakdownResponse breakdown =
                            new FinanceBreakdownResponse(
                                    rs.getString("breakdown_key"),
                                    rs.getLong("revenue_cents"),
                                    rs.getLong("payment_count"),
                                    0L
                            );

                    if ("module".equals(column)) {
                        day.byModule.add(breakdown);
                    } else {
                        day.byPaymentMethod.add(breakdown);
                    }
                }
        );
    }

    private Map<LocalDate, DailyAccumulator> initialDays(
            FinanceFilters filters
    ) {
        Map<LocalDate, DailyAccumulator> days = new LinkedHashMap<>();
        LocalDate date = filters.from();
        while (!date.isAfter(filters.to())) {
            days.put(date, new DailyAccumulator(date));
            date = date.plusDays(1);
        }

        return days;
    }

    private FinancePeriodResponse period(FinanceFilters filters) {
        return new FinancePeriodResponse(
                filters.from(),
                filters.to(),
                filters.timeZone()
        );
    }

    private MapSqlParameterSource params(FinanceFilters filters) {
        return new MapSqlParameterSource()
                .addValue(
                        "fromInstant",
                        OffsetDateTime.ofInstant(
                                filters.fromInstant(),
                                ZoneOffset.UTC
                        )
                )
                .addValue(
                        "toInstant",
                        OffsetDateTime.ofInstant(
                                filters.toExclusiveInstant(),
                                ZoneOffset.UTC
                        )
                )
                .addValue(
                        "module",
                        filters.module() == null
                                ? null
                                : filters.module().name()
                )
                .addValue(
                        "operationType",
                        filters.operationType() == null
                                ? null
                                : filters.operationType().name()
                )
                .addValue(
                        "paymentMethod",
                        filters.paymentMethod() == null
                                ? null
                                : filters.paymentMethod().databaseValue()
                )
                .addValue("timeZone", filters.timeZone())
                .addValue(
                        "search",
                        filters.normalizedSearch() == null
                                ? null
                                : "%" + filters.normalizedSearch() + "%"
                );
    }

    private long average(long totalRevenue, long paymentCount) {
        if (paymentCount == 0) {
            return 0L;
        }

        return totalRevenue / paymentCount;
    }

    private long basisPoints(long revenue, long totalRevenue) {
        if (totalRevenue == 0) {
            return 0L;
        }

        return BigInteger.valueOf(revenue)
                .multiply(BigInteger.valueOf(10_000L))
                .divide(BigInteger.valueOf(totalRevenue))
                .longValue();
    }

    private record SummaryTotals(
            long totalRevenueCents,
            long paymentCount,
            OffsetDateTime firstApprovedAt,
            OffsetDateTime lastApprovedAt
    ) {
    }

    private static final class SummaryTotalsRowMapper
            implements RowMapper<SummaryTotals> {

        @Override
        public SummaryTotals mapRow(
                ResultSet rs,
                int rowNum
        ) throws SQLException {
            return new SummaryTotals(
                    rs.getLong("total_revenue_cents"),
                    rs.getLong("payment_count"),
                    rs.getObject("first_approved_at", OffsetDateTime.class),
                    rs.getObject("last_approved_at", OffsetDateTime.class)
            );
        }
    }

    private final class TransactionRowMapper
            implements RowMapper<FinanceTransactionResponse> {

        @Override
        public FinanceTransactionResponse mapRow(
                ResultSet rs,
                int rowNum
        ) throws SQLException {
            return new FinanceTransactionResponse(
                    rs.getObject("payment_id", UUID.class),
                    rs.getObject("checkout_id", UUID.class),
                    rs.getString("operation_type"),
                    FinanceModule.valueOf(rs.getString("module")),
                    FinancePaymentMethod.valueOf(
                            rs.getString("payment_method")
                    ),
                    rs.getString("processing_mode"),
                    rs.getString("status"),
                    rs.getLong("amount_cents"),
                    rs.getObject("approved_at", OffsetDateTime.class),
                    rs.getLong("responsible_user_id"),
                    rs.getString("responsible_user_name"),
                    rs.getLong("operation_id"),
                    rs.getString("display_name"),
                    rs.getString("description"),
                    rs.getString("detail_path")
            );
        }
    }

    private final class DailyAccumulator {

        private final LocalDate date;
        private long totalRevenueCents;
        private long paymentCount;
        private final List<FinanceBreakdownResponse> byPaymentMethod =
                new ArrayList<>();
        private final List<FinanceBreakdownResponse> byModule =
                new ArrayList<>();

        private DailyAccumulator(LocalDate date) {
            this.date = date;
        }

        private FinanceDailyResponse toResponse() {
            return new FinanceDailyResponse(
                    date,
                    totalRevenueCents,
                    paymentCount,
                    average(totalRevenueCents, paymentCount),
                    byPaymentMethod,
                    byModule
            );
        }
    }
}
