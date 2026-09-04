package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class OperationalOrderRenderer {

    private static final Locale PORTUGUESE_BRAZIL =
            Locale.forLanguageTag("pt-BR");

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
        return render("PEDIDO - SERVIÇOS", tab, lines);
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
        return render("PEDIDO - SERVIÇO", tab, List.of(line));
    }

    private String render(
            String title,
            BarTabResponse tab,
            List<BarTabLineResponse> lines
    ) {
        StringBuilder text = new StringBuilder()
                .append(title)
                .append("\n\nCOMANDA:\n")
                .append(normalize(tab.name()))
                .append("\n\n");

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
