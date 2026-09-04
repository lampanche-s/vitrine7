package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PrePaymentNoteRenderer {

    private static final int COLUMNS = 48;
    private static final String DIVIDER = "-".repeat(COLUMNS);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final BusinessProperties properties;
    private final Clock clock;
    private final ZoneId businessZone;

    public PrePaymentNoteRenderer(
            BusinessProperties properties,
            Clock clock
    ) {
        this.properties = properties;
        this.clock = clock;
        this.businessZone = ZoneId.of(properties.businessTimeZone());
    }

    public String render(BarTabResponse tab) {
        List<String> lines = new ArrayList<>();
        String establishmentName = properties.establishment() == null
                ? "Vitrine 7"
                : properties.establishment().name();

        lines.add(center(establishmentName == null || establishmentName.isBlank()
                ? "Vitrine 7"
                : establishmentName));
        lines.add(center("CONFERENCIA DE CONSUMO"));
        lines.add(center("NAO E DOCUMENTO FISCAL"));
        lines.add(DIVIDER);
        lines.add("COMANDA: " + normalize(tab.name()));
        lines.add("DATA: " + DATE_TIME_FORMATTER.format(
                OffsetDateTime.now(clock).atZoneSameInstant(businessZone)
        ));
        lines.add(DIVIDER);

        for (BarTabLineResponse item : tab.lines()) {
            String quantity = item.quantity() + "x ";
            String total = currency(item.lineTotalCents());
            String description = normalize(item.itemName());
            int descriptionWidth = Math.max(8, COLUMNS - quantity.length() - total.length() - 2);
            List<String> wrapped = wrap(description, descriptionWidth);

            lines.add(quantity
                    + padRight(wrapped.isEmpty() ? "-" : wrapped.get(0), descriptionWidth)
                    + "  "
                    + total);

            for (int index = 1; index < wrapped.size(); index++) {
                lines.add(" ".repeat(quantity.length())
                        + padRight(wrapped.get(index), descriptionWidth));
            }

            lines.add("   " + currency(item.unitPriceCents()) + " cada");
        }

        lines.add(DIVIDER);
        lines.add(amount("TOTAL", currency(tab.totalCents())));
        lines.add(DIVIDER);
        lines.add(center("Confira os itens antes do pagamento."));
        lines.add(center("Obrigado!"));

        return String.join("\n", lines) + "\n";
    }

    private String currency(long cents) {
        return CURRENCY.format(BigDecimal.valueOf(cents, 2));
    }

    private String amount(String label, String value) {
        int spaces = Math.max(1, COLUMNS - label.length() - value.length());
        return label + " ".repeat(spaces) + value;
    }

    private String center(String value) {
        String normalized = normalize(value);
        int spaces = Math.max(0, (COLUMNS - normalized.length()) / 2);
        return " ".repeat(spaces) + normalized;
    }

    private String padRight(String value, int width) {
        return value.length() >= width
                ? value.substring(0, width)
                : value + " ".repeat(width - value.length());
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
}
