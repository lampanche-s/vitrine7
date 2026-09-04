package br.com.vitrine7.print.service;

import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CashClosingTextRenderer {

    private static final int COLUMNS = 48;
    private static final String DIVIDER = "-".repeat(COLUMNS);
    private static final String SEPARATOR = "=".repeat(COLUMNS);
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BusinessProperties properties;
    private final ZoneId businessZone;

    public CashClosingTextRenderer(BusinessProperties properties) {
        this.properties = properties;
        this.businessZone = ZoneId.of(properties.businessTimeZone());
    }

    public String render(CashClosingResponse report) {
        List<String> lines = new ArrayList<>();
        String establishment = properties.establishment() == null
                ? "Vitrine 7"
                : properties.establishment().name();

        lines.add(SEPARATOR);
        lines.add(center(hasText(establishment) ? establishment : "Vitrine 7"));
        lines.add(center("FECHAMENTO DE CAIXA"));
        lines.add(SEPARATOR);
        lines.add("OPERADOR: " + normalize(report.userName()));
        lines.add("DATA: " + DATE.format(report.businessDate()));
        lines.add(report.closed()
                ? "FECHADO: " + formatDateTime(report.closedAt())
                : "STATUS: AINDA NAO FECHADO");
        lines.add(DIVIDER);
        lines.add(amount("TOTAL BRUTO", currency(report.grossSalesCents())));
        lines.add(amount("TOTAL LIQUIDO", currency(report.totalReceivedCents())));
        lines.add(amount("ITENS", currency(report.itemSalesCents())));
        lines.add(amount("SERVICOS", currency(report.serviceSalesCents())));
        lines.add(amount("ESTORNOS", currency(report.reversedCents())));
        lines.add(amount("TROCO", currency(report.cashChangeCents())));
        lines.add("VENDAS: " + report.saleCount());
        lines.add("TICKET MEDIO: " + currency(report.averageTicketCents()));
        lines.add("PRIMEIRA VENDA: " + formatDateTime(report.firstSaleAt()));
        lines.add("ULTIMA VENDA: " + formatDateTime(report.lastSaleAt()));
        lines.add(DIVIDER);
        lines.add("FORMAS DE PAGAMENTO");

        if (report.paymentBreakdown().isEmpty()) {
            lines.add("Nenhuma venda concluida.");
        } else {
            for (CashClosingResponse.PaymentBreakdown payment : report.paymentBreakdown()) {
                lines.add(amount(
                        paymentLabel(payment.method()) + " (" + payment.saleCount() + ")",
                        currency(payment.amountCents())
                ));
            }
        }

        lines.add(DIVIDER);
        lines.add("OPERACOES DO DIA");

        if (report.operations().isEmpty()) {
            lines.add("Nenhuma operacao no periodo.");
        } else {
            for (CashClosingResponse.Operation operation : report.operations()) {
                lines.add(DIVIDER);
                lines.add("COMANDA #" + operation.operationId()
                        + " | " + normalize(operation.displayName()));
                lines.add(formatDateTime(operation.completedAt())
                        + " | " + paymentLabel(operation.paymentMethod())
                        + " | " + statusLabel(operation.paymentStatus()));
                lines.add(amount("TOTAL", currency(operation.amountCents())));

                for (CashClosingResponse.Line item : operation.lines()) {
                    String type = entryTypeLabel(item.entryType());
                    String prefix = "[" + type + "] " + item.quantity() + "x ";
                    String total = currency(item.lineTotalCents());
                    int descriptionWidth = Math.max(
                            8,
                            COLUMNS - prefix.length() - total.length() - 2
                    );
                    List<String> wrapped = wrap(item.itemName(), descriptionWidth);
                    lines.add(prefix
                            + padRight(wrapped.isEmpty() ? "-" : wrapped.get(0), descriptionWidth)
                            + "  "
                            + total);
                    for (int index = 1; index < wrapped.size(); index++) {
                        lines.add(" ".repeat(prefix.length())
                                + padRight(wrapped.get(index), descriptionWidth));
                    }
                    lines.add("   " + currency(item.unitPriceCents()) + " cada");
                }
            }
        }

        lines.add(SEPARATOR);
        lines.add(center("FIM DO FECHAMENTO"));
        lines.add(SEPARATOR);
        return String.join("\n", lines) + "\n";
    }

    private String paymentLabel(String method) {
        return switch (method) {
            case "CASH" -> "DINHEIRO";
            case "PIX" -> "PIX";
            case "CREDIT_CARD" -> "CREDITO";
            case "DEBIT_CARD" -> "DEBITO";
            default -> normalize(method).toUpperCase(Locale.ROOT);
        };
    }

    private String statusLabel(String status) {
        return "REVERSED".equals(status) ? "ESTORNADA" : "APROVADA";
    }

    private String entryTypeLabel(String entryType) {
        return "SERVICE".equals(entryType) ? "SERVICO" : "ITEM";
    }

    private String formatDateTime(java.time.OffsetDateTime value) {
        return value == null
                ? "-"
                : DATE_TIME.format(value.atZoneSameInstant(businessZone));
    }

    private String currency(long cents) {
        return CURRENCY.format(BigDecimal.valueOf(cents, 2));
    }

    private String amount(String label, String value) {
        String normalized = normalize(label) + ":";
        int spaces = Math.max(1, COLUMNS - normalized.length() - value.length());
        return normalized + " ".repeat(spaces) + value;
    }

    private String center(String value) {
        String normalized = normalize(value);
        int spaces = Math.max(0, (COLUMNS - normalized.length()) / 2);
        return " ".repeat(spaces) + normalized;
    }

    private String padRight(String value, int width) {
        String normalized = normalize(value);
        return normalized.length() >= width
                ? normalized.substring(0, width)
                : normalized + " ".repeat(width - normalized.length());
    }

    private List<String> wrap(String value, int width) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (!current.isEmpty() && current.length() + word.length() + 1 > width) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            if (word.length() <= width) {
                current.append(word);
            } else {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                for (int offset = 0; offset < word.length(); offset += width) {
                    result.add(word.substring(offset, Math.min(word.length(), offset + width)));
                }
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
