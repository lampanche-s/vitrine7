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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CashClosingService {

    private static final LocalTime OPERATIONAL_DAY_START = LocalTime.of(5, 0);

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
        OffsetDateTime now = OffsetDateTime.now(clock.withZone(businessZone));

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
                current.itemSalesCents(),
                current.serviceSalesCents(),
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
        OperationalPeriod period = resolveOperationalPeriod(day);
        LocalDate businessDate = period.businessDate();

        List<CashClosingResponse.Operation> rawOperations =
                repository.operations(
                        principal.getId(),
                        period.start(),
                        period.endExclusive()
                );
        List<Long> operationIds = rawOperations.stream()
                .map(CashClosingResponse.Operation::operationId)
                .distinct()
                .toList();
        Map<Long, List<CashClosingResponse.Line>> linesByOperation =
                repository.operationLines(operationIds).stream()
                        .collect(Collectors.groupingBy(
                                CashClosingRepository.OperationLine::operationId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        line -> new CashClosingResponse.Line(
                                                line.entryType(),
                                                line.itemName(),
                                                line.quantity(),
                                                line.unitPriceCents(),
                                                line.lineTotalCents()
                                        ),
                                        Collectors.toList()
                                )
                        ));

        List<CashClosingResponse.Operation> operations = rawOperations.stream()
                .map(operation -> new CashClosingResponse.Operation(
                        operation.operationId(),
                        operation.displayName(),
                        operation.completedAt(),
                        operation.paymentMethod(),
                        operation.paymentStatus(),
                        operation.amountCents(),
                        operation.cashReceivedCents(),
                        operation.cashChangeCents(),
                        List.copyOf(linesByOperation.getOrDefault(
                                operation.operationId(),
                                List.of()
                        ))
                ))
                .toList();

        long totalReceived = 0L;
        long itemSalesCents = 0L;
        long serviceSalesCents = 0L;
        long saleCount = 0L;
        long reversedCents = 0L;
        long reversedCount = 0L;
        long cashReceivedCents = 0L;
        long cashChangeCents = 0L;
        OffsetDateTime firstSaleAt = null;
        OffsetDateTime lastSaleAt = null;

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

            TypeAmounts typeAmounts = allocateByEntryType(operation);
            itemSalesCents += typeAmounts.itemCents();
            serviceSalesCents += typeAmounts.serviceCents();

            cashReceivedCents += operation.cashReceivedCents();
            cashChangeCents += operation.cashChangeCents();

        }

        List<CashClosingResponse.PaymentBreakdown> paymentBreakdown =
                repository.paymentBreakdown(
                        principal.getId(),
                        period.start(),
                        period.endExclusive()
                );

        long grossSalesCents = totalReceived + reversedCents;
        long averageTicketCents = saleCount == 0L
                ? 0L
                : Math.round((double) totalReceived / saleCount);

        CashClosingRepository.OpenCommandsSummary openCommands =
                repository.openCommands(
                        principal.getId(),
                        period.start(),
                        period.endExclusive()
                );

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
                itemSalesCents,
                serviceSalesCents,
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

    private TypeAmounts allocateByEntryType(CashClosingResponse.Operation operation) {
        long itemSubtotal = operation.lines().stream()
                .filter(line -> "ITEM".equals(line.entryType()))
                .mapToLong(CashClosingResponse.Line::lineTotalCents)
                .sum();
        long serviceSubtotal = operation.lines().stream()
                .filter(line -> "SERVICE".equals(line.entryType()))
                .mapToLong(CashClosingResponse.Line::lineTotalCents)
                .sum();
        long classifiedSubtotal = itemSubtotal + serviceSubtotal;

        if (classifiedSubtotal <= 0L) {
            return new TypeAmounts(0L, 0L);
        }
        if (serviceSubtotal == 0L) {
            return new TypeAmounts(operation.amountCents(), 0L);
        }
        if (itemSubtotal == 0L) {
            return new TypeAmounts(0L, operation.amountCents());
        }

        long itemAmount = Math.round(
                (double) operation.amountCents() * itemSubtotal / classifiedSubtotal
        );
        itemAmount = Math.max(0L, Math.min(operation.amountCents(), itemAmount));
        return new TypeAmounts(
                itemAmount,
                operation.amountCents() - itemAmount
        );
    }

    OperationalPeriod resolveOperationalPeriod(CashClosingDay day) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(businessZone));
        LocalDate currentBusinessDate = now.toLocalDate();
        if (now.toLocalTime().isBefore(OPERATIONAL_DAY_START)) {
            currentBusinessDate = currentBusinessDate.minusDays(1);
        }

        LocalDate businessDate = day == CashClosingDay.YESTERDAY
                ? currentBusinessDate.minusDays(1)
                : currentBusinessDate;
        return operationalPeriodFor(businessDate);
    }

    OperationalPeriod operationalPeriodFor(LocalDate businessDate) {
        OffsetDateTime start = businessDate
                .atTime(OPERATIONAL_DAY_START)
                .atZone(businessZone)
                .toOffsetDateTime();
        OffsetDateTime endExclusive = businessDate
                .plusDays(1)
                .atTime(OPERATIONAL_DAY_START)
                .atZone(businessZone)
                .toOffsetDateTime();
        return new OperationalPeriod(businessDate, start, endExclusive);
    }

    private record TypeAmounts(
            long itemCents,
            long serviceCents
    ) {
    }

    record OperationalPeriod(
            LocalDate businessDate,
            OffsetDateTime start,
            OffsetDateTime endExclusive
    ) {
    }
}
