package br.com.vitrine7.payment.terminal.adapter;

public record TerminalAdapterResult(
        boolean approved,
        String providerTransactionId,
        String responseCode,
        String responseMessage
) {
}
