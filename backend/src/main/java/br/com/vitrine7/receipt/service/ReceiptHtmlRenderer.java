package br.com.vitrine7.receipt.service;

import br.com.vitrine7.receipt.dto.ReceiptEstablishmentResponse;
import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptPaymentResponse;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class ReceiptHtmlRenderer {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX");

    public String render(ReceiptResponse receipt) {
        StringBuilder html = new StringBuilder(8192);

        html.append("""
                <!doctype html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <title>Comprovante nao fiscal</title>
                    <style>
                        @page {
                            size: 80mm auto;
                            margin: 0;
                        }

                        * {
                            box-sizing: border-box;
                        }

                        body {
                            width: 80mm;
                            margin: 0 auto;
                            padding: 4mm 5mm 6mm;
                            font-family: "Courier New", Courier, monospace;
                            font-size: 10.5px;
                            line-height: 1.22;
                            color: #000;
                            background: #fff;
                        }

                        .receipt {
                            width: 70mm;
                            margin: 0 auto;
                        }

                        .center {
                            text-align: center;
                        }

                        .right {
                            text-align: right;
                        }

                        .bold {
                            font-weight: 700;
                        }

                        .uppercase {
                            text-transform: uppercase;
                        }

                        .store-name {
                            font-size: 13px;
                            line-height: 1.15;
                        }

                        .store-detail {
                            font-size: 9.5px;
                            overflow-wrap: anywhere;
                        }

                        .receipt-title {
                            font-size: 12px;
                            letter-spacing: 0;
                            line-height: 1.15;
                        }

                        .notice {
                            font-size: 9.5px;
                            line-height: 1.25;
                        }

                        .section-title {
                            margin: 0 0 3px;
                            font-size: 9.5px;
                            font-weight: 700;
                            text-transform: uppercase;
                        }

                        .line {
                            border-top: 1px solid #000;
                            margin: 5px 0;
                            height: 0;
                        }

                        .line-light {
                            border-top: 1px dashed #000;
                            margin: 4px 0;
                            height: 0;
                        }

                        .row {
                            display: grid;
                            grid-template-columns: minmax(0, 1fr) auto;
                            gap: 6px;
                            align-items: start;
                        }

                        .row span:first-child {
                            min-width: 0;
                            overflow-wrap: anywhere;
                        }

                        .row span:last-child {
                            white-space: nowrap;
                            text-align: right;
                        }

                        .label-row {
                            display: grid;
                            grid-template-columns: 22mm minmax(0, 1fr);
                            gap: 3px;
                            margin: 1px 0;
                        }

                        .label-row span:first-child {
                            font-weight: 700;
                            text-transform: uppercase;
                        }

                        .label-row span:last-child {
                            overflow-wrap: anywhere;
                            text-align: right;
                        }

                        .item {
                            margin: 0 0 5px;
                            break-inside: avoid;
                        }

                        .item-name {
                            font-weight: 700;
                            text-transform: uppercase;
                            overflow-wrap: anywhere;
                        }

                        .item-category {
                            font-size: 9.5px;
                            overflow-wrap: anywhere;
                        }

                        .item-values {
                            margin-top: 1px;
                        }

                        .total-row {
                            margin-top: 2px;
                            font-size: 13px;
                            line-height: 1.2;
                        }

                        .footer {
                            margin-top: 6px;
                        }

                        @media screen {
                            html {
                                background: #e5e5e5;
                            }

                            body {
                                min-height: 100vh;
                            }
                        }
                    </style>
                </head>
                <body>
                <main class="receipt">
                """);

        appendEstablishment(html, receipt.establishment());
        div(html, "line", "");
        textLine(html, "div", "center bold uppercase receipt-title", "RECIBO GERAL NAO FISCAL");
        textLine(html, "div", "center notice uppercase", receipt.title());
        textLine(html, "div", "center notice", "Documento geral nao fiscal");
        div(html, "line", "");

        textLine(html, "div", "section-title", "Dados da operacao");
        label(html, "Operacao", operationLabel(receipt));
        label(html, "Checkout", receipt.operation().checkoutId().toString());
        label(html, "ID", String.valueOf(receipt.operation().operationId()));
        label(html, "Status", receipt.operation().status());
        label(html, "Data/Hora", receipt.issuedAt().format(DATE_TIME_FORMATTER));

        if (receipt.operation().responsibleUserName() != null) {
            label(html, "Operador", receipt.operation().responsibleUserName());
        }

        div(html, "line", "");
        textLine(html, "div", "section-title", "Itens");
        for (ReceiptLineResponse line : receipt.lines()) {
            html.append("<div class=\"item\">");
            textLine(html, "div", "item-name", line.description());
            if (line.category() != null) {
                textLine(html, "div", "item-category", line.category());
            }
            row(
                    html,
                    line.quantity()
                            + " x "
                            + money(line.unitPriceCents()),
                    money(line.totalCents()),
                    false,
                    "item-values"
            );
            html.append("</div>\n");
        }

        div(html, "line", "");
        textLine(html, "div", "section-title", "Resumo financeiro");
        row(html, "Subtotal", money(receipt.subtotalCents()));
        row(html, "Desconto", money(receipt.discountCents()));
        row(html, "TOTAL", money(receipt.totalCents()), true, "total-row");

        div(html, "line", "");
        textLine(html, "div", "section-title", "Pagamento");
        ReceiptPaymentResponse payment = receipt.payment();
        label(html, "Metodo", payment.method());
        label(html, "Modo", payment.processingMode());
        label(html, "Pagamento ID", payment.paymentId().toString());
        if (payment.terminalProvider() != null) {
            label(html, "Provedor", payment.terminalProvider());
        }
        if (payment.cashReceivedCents() != null) {
            row(html, "Recebido", money(payment.cashReceivedCents()));
        }
        if (payment.cashChangeCents() != null) {
            row(html, "Troco", money(payment.cashChangeCents()));
        }

        div(html, "line", "");
        textLine(html, "div", "center bold uppercase notice", receipt.nonFiscalNotice());
        textLine(html, "div", "center notice", "Nao substitui documento fiscal");
        textLine(html, "div", "center notice footer", "Obrigado pela preferencia");
        html.append("</main>\n</body>\n</html>\n");

        return html.toString();
    }

    private void appendEstablishment(
            StringBuilder html,
            ReceiptEstablishmentResponse establishment
    ) {
        textLine(html, "div", "center bold uppercase store-name", establishment.name());
        if (establishment.document() != null) {
            textLine(html, "div", "center store-detail", establishment.document());
        }
        if (establishment.phone() != null) {
            textLine(html, "div", "center store-detail", establishment.phone());
        }
        if (establishment.address() != null) {
            textLine(html, "div", "center store-detail", establishment.address());
        }
    }

    private String operationLabel(ReceiptResponse receipt) {
        return "BAR_COMMAND".equals(receipt.operation().type())
                ? receipt.operation().displayName()
                : receipt.operation().type();
    }

    private void label(
            StringBuilder html,
            String label,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        html.append("<div class=\"label-row\"><span>")
                .append(escape(label))
                .append("</span><span>")
                .append(escape(value))
                .append("</span></div>\n");
    }

    private void row(
            StringBuilder html,
            String left,
            String right
    ) {
        row(html, left, right, false);
    }

    private void row(
            StringBuilder html,
            String left,
            String right,
            boolean bold
    ) {
        row(html, left, right, bold, null);
    }

    private void row(
            StringBuilder html,
            String left,
            String right,
            boolean bold,
            String cssClass
    ) {
        html.append("<div class=\"row");
        if (bold) {
            html.append(" bold");
        }
        if (cssClass != null && !cssClass.isBlank()) {
            html.append(" ")
                    .append(cssClass);
        }
        html.append("\"><span>")
                .append(escape(left))
                .append("</span><span>")
                .append(escape(right))
                .append("</span></div>\n");
    }

    private void textLine(
            StringBuilder html,
            String tag,
            String cssClass,
            String text
    ) {
        if (text == null || text.isBlank()) {
            return;
        }

        html.append("<")
                .append(tag);
        if (cssClass != null && !cssClass.isBlank()) {
            html.append(" class=\"")
                    .append(cssClass)
                    .append("\"");
        }
        html.append(">")
                .append(escape(text))
                .append("</")
                .append(tag)
                .append(">\n");
    }

    private void div(
            StringBuilder html,
            String cssClass,
            String text
    ) {
        textLine(html, "div", cssClass, text.isEmpty() ? " " : text);
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    private String money(long cents) {
        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(PT_BR);
        return formatter.format(cents / 100.0);
    }
}
