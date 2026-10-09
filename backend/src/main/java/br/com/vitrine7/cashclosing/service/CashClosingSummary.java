package br.com.vitrine7.cashclosing.service;

import br.com.vitrine7.cashclosing.dto.CashClosingResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CashClosingSummary(List<Entry> items, List<Entry> services) {

    public record Entry(String description, long quantity, long totalCents) {
    }

    public static CashClosingSummary from(List<CashClosingResponse.Operation> operations) {
        Map<String, Entry> items = new LinkedHashMap<>();
        Map<String, Entry> services = new LinkedHashMap<>();

        for (CashClosingResponse.Operation operation : operations) {
            if (!"APPROVED".equals(operation.paymentStatus())) {
                continue;
            }

            long itemSubtotal = subtotal(operation, "ITEM");
            long serviceSubtotal = subtotal(operation, "SERVICE");
            long subtotal = itemSubtotal + serviceSubtotal;
            long itemAmount = 0L;
            long serviceAmount = 0L;
            if (subtotal > 0L) {
                itemAmount = serviceSubtotal == 0L
                        ? operation.amountCents()
                        : Math.round((double) operation.amountCents() * itemSubtotal / subtotal);
                itemAmount = Math.max(0L, Math.min(operation.amountCents(), itemAmount));
                serviceAmount = operation.amountCents() - itemAmount;
            }

            accumulate(items, operation, "ITEM", itemSubtotal, itemAmount);
            accumulate(services, operation, "SERVICE", serviceSubtotal, serviceAmount);
        }

        Comparator<Entry> byDescription = Comparator.comparing(
                Entry::description, String.CASE_INSENSITIVE_ORDER
        ).thenComparing(Entry::description);
        return new CashClosingSummary(
                items.values().stream().sorted(byDescription).toList(),
                services.values().stream().sorted(byDescription).toList()
        );
    }

    public long itemTotalCents() {
        return items.stream().mapToLong(Entry::totalCents).sum();
    }

    public long serviceTotalCents() {
        return services.stream().mapToLong(Entry::totalCents).sum();
    }

    private static long subtotal(CashClosingResponse.Operation operation, String type) {
        return operation.lines().stream()
                .filter(line -> type.equals(line.entryType()))
                .mapToLong(CashClosingResponse.Line::lineTotalCents)
                .sum();
    }

    private static void accumulate(
            Map<String, Entry> result,
            CashClosingResponse.Operation operation,
            String type,
            long subtotal,
            long amount
    ) {
        Map<String, Entry> grouped = new LinkedHashMap<>();
        for (CashClosingResponse.Line line : operation.lines()) {
            if (type.equals(line.entryType())) {
                merge(grouped, new Entry(line.itemName(), line.quantity(), line.lineTotalCents()));
            }
        }

        long cumulativeSubtotal = 0L;
        long allocated = 0L;
        for (Entry entry : grouped.values()) {
            cumulativeSubtotal += entry.totalCents();
            // Cumulative rounding distributes discounts without losing cents.
            long cumulativeAmount = subtotal <= 0L ? 0L : BigDecimal.valueOf(amount)
                    .multiply(BigDecimal.valueOf(cumulativeSubtotal))
                    .divide(BigDecimal.valueOf(subtotal), 0, RoundingMode.HALF_UP)
                    .longValueExact();
            merge(result, new Entry(entry.description(), entry.quantity(), cumulativeAmount - allocated));
            allocated = cumulativeAmount;
        }
    }

    private static void merge(Map<String, Entry> entries, Entry entry) {
        entries.merge(entry.description(), entry, (first, second) -> new Entry(
                first.description(),
                first.quantity() + second.quantity(),
                first.totalCents() + second.totalCents()
        ));
    }
}
