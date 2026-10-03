package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class OperationalOrderRenderer {

    private static final Locale PORTUGUESE_BRAZIL =
            Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ZoneId businessZone;

    public OperationalOrderRenderer(BusinessProperties properties) {
        this.businessZone = ZoneId.of(properties.businessTimeZone());
    }

    public String renderAddedEntry(
            String tabName,
            CatalogEntryType entryType,
            String itemName,
            int addedQuantity,
            long unitPriceCents,
            String operatorName,
            OffsetDateTime addedAt
    ) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(PORTUGUESE_BRAZIL);
        String type = entryType == CatalogEntryType.SERVICE ? "SERVIÇO" : "ITEM";
        return "PEDIDO - " + type + "\n\n"
                + "COMANDA: " + normalize(tabName) + "\n"
                + "DATA: " + DATE_TIME.format(addedAt.atZoneSameInstant(businessZone)) + "\n"
                + "OPERADOR: " + normalize(operatorName) + "\n\n"
                + "TIPO: " + type + "\n"
                + addedQuantity + "x " + normalize(itemName).toUpperCase(PORTUGUESE_BRAZIL) + "\n"
                + "VALOR UNITARIO: " + currency.format(BigDecimal.valueOf(unitPriceCents, 2)) + "\n"
                + "VALOR: " + currency.format(BigDecimal.valueOf(
                        Math.multiplyExact(unitPriceCents, addedQuantity), 2)) + "\n";
    }

    public String renderItems(
            BarTabResponse tab,
            List<BarTabLineResponse> lines
    ) {
        return render("PEDIDO - ITENS", tab, lines);
    }

    public String renderServices(
            BarTabResponse tab,
            List<BarTabLineResponse> lines
    ) {
        return render("PEDIDO - SERVIÇOS", tab, lines, true);
    }

    public String renderSingleItem(
            BarTabResponse tab,
            BarTabLineResponse line
    ) {
        return render("PEDIDO - ITEM", tab, List.of(line));
    }

    public String renderSingleService(
            BarTabResponse tab,
            BarTabLineResponse line
    ) {
        return render("PEDIDO - SERVIÇO", tab, List.of(line), true);
    }

    private String render(
            String title,
            BarTabResponse tab,
            List<BarTabLineResponse> lines
    ) {
        return render(title, tab, lines, false);
    }

    private String render(
            String title,
            BarTabResponse tab,
            List<BarTabLineResponse> lines,
            boolean includeVehicle
    ) {
        StringBuilder text = new StringBuilder()
                .append(title)
                .append("\n\nCOMANDA:\n")
                .append(normalize(tab.name()))
                .append("\n\n");

        if (includeVehicle) {
            text.append("VEÍCULO: ").append(normalize(tab.vehicleName())).append('\n')
                    .append("PLACA: ").append(normalize(tab.vehiclePlate())).append("\n\n");
        }

        for (BarTabLineResponse line : lines) {
            text.append(line.quantity())
                    .append("x ")
                    .append(normalize(line.itemName()).toUpperCase(PORTUGUESE_BRAZIL))
                    .append('\n');
        }

        return text.toString();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ").trim();
    }
}
