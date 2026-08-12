package br.com.vitrine7.cashclosing.service;

import br.com.vitrine7.cashclosing.dto.CashClosingDay;
import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import br.com.vitrine7.cashclosing.repository.CashClosingRepository;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashClosingService {

    private final CashClosingRepository repository;
    private final Clock clock;
    private final ZoneId businessZone;

    public CashClosingService(
            CashClosingRepository repository,
            Clock clock,
            BusinessProperties properties
    ) {
        this.repository = repository;
        this.clock = clock;
        this.businessZone = ZoneId.of(properties.businessTimeZone());
    }

    @Transactional(readOnly = true)
    public CashClosingResponse get(
            CashClosingDay day,
            VitrineUserPrincipal principal
    ) {
        return build(day, principal, null);
    }

    @Transactional
    public CashClosingResponse close(
            CashClosingDay day,
            VitrineUserPrincipal principal
    ) {
        CashClosingResponse current = build(day, principal, null);
        OffsetDateTime now = OffsetDateTime.now(clock);

        repository.upsert(
                principal.getId(),
                current.businessDate(),
                current,
                now
        );

        return new CashClosingResponse(
                current.businessDate(),
                current.userName(),
                true,
                now,
                current.grossSalesCents(),
                current.totalReceivedCents(),
                current.saleCount(),
                current.averageTicketCents(),
                current.reversedCents(),
                current.reversedCount(),
                current.cashReceivedCents(),
                current.cashChangeCents(),
                current.openCommandCount(),
                current.openCommandAmountCents(),
                current.firstSaleAt(),
                current.lastSaleAt(),
                current.paymentBreakdown(),
                current.operations()
        );
    }

    private CashClosingResponse build(
            CashClosingDay day,
            VitrineUserPrincipal principal,
            OffsetDateTime forcedClosedAt
    ) {
        LocalDate today = LocalDate.now(clock.withZone(businessZone));
        LocalDate businessDate = day == CashClosingDay.YESTERDAY
                ? today.minusDays(1)
                : today;

        OffsetDateTime from = businessDate
                .atStartOfDay(businessZone)
                .toInstant()
                .atOffset(ZoneOffset.UTC);
        OffsetDateTime to = businessDate
                .plusDays(1)
                .atStartOfDay(businessZone)
                .toInstant()
                .atOffset(ZoneOffset.UTC);

        List<CashClosingResponse.Operation> operations =
                repository.operations(principal.getId(), from, to);

        long totalReceived = 0L;
        long saleCount = 0L;
        long reversedCents = 0L;
        long reversedCount = 0L;
        long cashReceivedCents = 0L;
        long cashChangeCents = 0L;
        OffsetDateTime firstSaleAt = null;
        OffsetDateTime lastSaleAt = null;
        Map<String, MutableBreakdown> breakdown = new LinkedHashMap<>();

        for (CashClosingResponse.Operation operation : operations) {
            if (firstSaleAt == null || operation.completedAt().isBefore(firstSaleAt)) {
                firstSaleAt = operation.completedAt();
            }
            if (lastSaleAt == null || operation.completedAt().isAfter(lastSaleAt)) {
                lastSaleAt = operation.completedAt();
            }

            if ("REVERSED".equals(operation.paymentStatus())) {
                reversedCents += operation.amountCents();
                reversedCount++;
                continue;
            }

            totalReceived += operation.amountCents();
            saleCount++;

            if ("CASH".equals(operation.paymentMethod())) {
                cashReceivedCents += operation.cashReceivedCents();
                cashChangeCents += operation.cashChangeCents();
            }

            MutableBreakdown item = breakdown.computeIfAbsent(
                    operation.paymentMethod(),
                    ignored -> new MutableBreakdown()
            );
            item.amountCents += operation.amountCents();
            item.saleCount++;
        }

        List<CashClosingResponse.PaymentBreakdown> paymentBreakdown =
                new ArrayList<>();
        breakdown.forEach((method, value) -> paymentBreakdown.add(
                new CashClosingResponse.PaymentBreakdown(
                        method,
                        value.amountCents,
                        value.saleCount
                )
        ));

        long grossSalesCents = totalReceived + reversedCents;
        long averageTicketCents = saleCount == 0L
                ? 0L
                : Math.round((double) totalReceived / saleCount);

        CashClosingRepository.OpenCommandsSummary openCommands =
                repository.openCommands(principal.getId(), from, to);

        OffsetDateTime closedAt = forcedClosedAt != null
                ? forcedClosedAt
                : repository.findClosedAt(principal.getId(), businessDate)
                        .orElse(null);

        return new CashClosingResponse(
                businessDate,
                principal.getName(),
                closedAt != null,
                closedAt,
                grossSalesCents,
                totalReceived,
                saleCount,
                averageTicketCents,
                reversedCents,
                reversedCount,
                cashReceivedCents,
                cashChangeCents,
                openCommands.count(),
                openCommands.amountCents(),
                firstSaleAt,
                lastSaleAt,
                List.copyOf(paymentBreakdown),
                List.copyOf(operations)
        );
    }

    private static final class MutableBreakdown {
        private long amountCents;
        private long saleCount;
    }
}
