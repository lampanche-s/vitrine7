package br.com.vitrine7.print.service;

import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ReceiptTextRenderer {

    private static final int COLUMNS = 48;
    private static final String SEPARATOR = "=".repeat(COLUMNS);
    private static final String DIVIDER = "-".repeat(COLUMNS);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat DECIMAL =
            NumberFormat.getNumberInstance(new Locale("pt", "BR"));

    public ReceiptTextRenderer() {
        DECIMAL.setMinimumFractionDigits(2);
        DECIMAL.setMaximumFractionDigits(2);
    }

    public String render(ReceiptResponse receipt) {
        List<String> lines = new ArrayList<>();

        lines.add(SEPARATOR);
        lines.addAll(center(receipt.establishment().name()));
        if (hasText(receipt.establishment().address())) {
            lines.addAll(center(receipt.establishment().address()));
        }
        lines.add(SEPARATOR);

        String issuedAt = DATE_TIME_FORMATTER.format(receipt.issuedAt());
        String cupom = String.format("%04d", receipt.operation().operationId());
        String documentLine = "DOC. NAO FISCAL | " + issuedAt + " | CUPOM: " + cupom;

        if (documentLine.length() <= COLUMNS) {
            lines.add(documentLine);
        } else {
            lines.add("DOC. NAO FISCAL | " + issuedAt);
            lines.add(right("CUPOM: " + cupom, COLUMNS));
        }

        lines.add(DIVIDER);
        lines.addAll(labelValue("Tipo", operationLabel(receipt.operation().type())));
        if (hasText(receipt.operation().responsibleUserName())) {
            lines.addAll(labelValue("Responsavel", receipt.operation().responsibleUserName()));
        }
        if (hasText(receipt.operation().vehicleName())) {
            lines.addAll(labelValue("Veiculo", receipt.operation().vehicleName()));
            lines.addAll(labelValue("Placa", receipt.operation().vehiclePlate()));
        }

        boolean reversed = receipt.payments().stream()
                .allMatch(payment -> "REVERSED".equals(payment.status()));
        if (reversed) {
            lines.addAll(labelValue("Pagamento", "Estornado"));
            receipt.payments().stream()
                    .map(br.com.vitrine7.receipt.dto.ReceiptPaymentResponse::reversedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(java.time.OffsetDateTime::compareTo)
                    .ifPresent(value -> lines.addAll(labelValue(
                            "Estornado em",
                            DATE_TIME_FORMATTER.format(value)
                    )));
            receipt.payments().stream()
                    .map(br.com.vitrine7.receipt.dto.ReceiptPaymentResponse::reversalReason)
                    .filter(this::hasText)
                    .findFirst()
                    .ifPresent(value -> lines.addAll(labelValue("Motivo", value)));
        }

        lines.add(DIVIDER);
        lines.add(String.join(" ",
                left("COD", 3),
                left("DESCRICAO", 23),
                right("QTD", 3),
                right("VL.UN", 7),
                right("VL.TOT", 8)
        ));
        lines.add(DIVIDER);

        int index = 1;
        for (ReceiptLineResponse item : receipt.lines()) {
            List<String> descriptions = wrap(item.description(), 23);
            String firstDescription = descriptions.isEmpty() ? "-" : descriptions.get(0);

            lines.add(String.join(" ",
                    left(String.format("%03d", index), 3),
                    left(firstDescription, 23),
                    right(String.valueOf(item.quantity()), 3),
                    right(decimal(item.unitPriceCents()), 7),
                    right(decimal(item.totalCents()), 8)
            ));

            for (int continuation = 1; continuation < descriptions.size(); continuation++) {
                lines.add(String.join(" ",
                        " ".repeat(3),
                        left(descriptions.get(continuation), 23),
                        " ".repeat(3),
                        " ".repeat(7),
                        " ".repeat(8)
                ));
            }

            index++;
        }

        lines.add(DIVIDER);
        lines.add(amount("Subtotal", brl(receipt.subtotalCents())));
        lines.add(amount("Desconto", brl(receipt.discountCents())));
        lines.add(amount("Total", brl(receipt.totalCents())));
        lines.add(DIVIDER);
        for (var payment : receipt.payments()) {
            String label = receipt.payments().size() == 1
                    ? "Forma de pagamento"
                    : paymentLabel(payment.method());
            String value = receipt.payments().size() == 1
                    ? paymentLabel(payment.method())
                    : brl(payment.approvedAmountCents());
            lines.addAll(labelValue(label, value));

            if (payment.cashReceivedCents() != null) {
                lines.add(amount("Valor recebido", brl(payment.cashReceivedCents())));
            }
            if (payment.cashChangeCents() != null) {
                lines.add(amount("Troco", brl(payment.cashChangeCents())));
            }
        }

        lines.add(DIVIDER);
        lines.addAll(center("Obrigado pela preferencia!"));
        lines.addAll(center("Volte Sempre / Vitrine 7"));
        lines.add(SEPARATOR);

        return String.join("\n", lines) + "\n";
    }

    private String operationLabel(String type) {
        return "BAR_COMMAND".equals(type) ? "Comanda" : type;
    }

    private String paymentLabel(String method) {
        return switch (method) {
            case "CASH" -> "Dinheiro";
            case "PIX" -> "Pix";
            case "CREDIT", "CREDIT_CARD" -> "Credito";
            case "DEBIT", "DEBIT_CARD" -> "Debito";
            default -> method;
        };
    }

    private List<String> labelValue(String label, String value) {
        String prefix = normalize(label).toUpperCase(Locale.ROOT) + ": ";
        int width = Math.max(12, COLUMNS - prefix.length());
        List<String> wrapped = wrap(value, width);
        List<String> result = new ArrayList<>();

        for (int index = 0; index < wrapped.size(); index++) {
            result.add(index == 0
                    ? prefix + wrapped.get(index)
                    : " ".repeat(prefix.length()) + wrapped.get(index));
        }

        return result;
    }

    private String amount(String label, String value) {
        String normalizedLabel = normalize(label).toUpperCase(Locale.ROOT) + ":";
        int spaces = Math.max(1, COLUMNS - normalizedLabel.length() - value.length());
        return normalizedLabel + " ".repeat(spaces) + value;
    }

    private List<String> center(String value) {
        List<String> result = new ArrayList<>();
        for (String line : wrap(value, COLUMNS)) {
            result.add(" ".repeat(Math.max(0, (COLUMNS - line.length()) / 2)) + line);
        }
        return result;
    }

    private List<String> wrap(String value, int width) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : normalized.split(" ")) {
            if (word.length() > width) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                for (int offset = 0; offset < word.length(); offset += width) {
                    lines.add(word.substring(offset, Math.min(word.length(), offset + width)));
                }
                continue;
            }

            String candidate = current.isEmpty()
                    ? word
                    : current + " " + word;

            if (candidate.length() > width && !current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    private String left(String value, int width) {
        String normalized = normalize(value);
        return normalized.length() <= width
                ? normalized + " ".repeat(width - normalized.length())
                : normalized.substring(0, width);
    }

    private String right(String value, int width) {
        String normalized = normalize(value);
        return normalized.length() <= width
                ? " ".repeat(width - normalized.length()) + normalized
                : normalized;
    }

    private String brl(long cents) {
        return CURRENCY.format(BigDecimal.valueOf(cents, 2));
    }

    private String decimal(long cents) {
        return DECIMAL.format(
                BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
