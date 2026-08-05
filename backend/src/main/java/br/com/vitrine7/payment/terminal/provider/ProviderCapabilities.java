package br.com.vitrine7.payment.terminal.provider;

public record ProviderCapabilities(
        boolean credit,
        boolean debit,
        boolean pix,
        boolean cancellation,
        boolean query,
        boolean synchronous,
        boolean asynchronous,
        boolean requiresPhysicalTerminal,
        boolean requiresWebhook,
        PaymentProviderImplementationStatus implementationStatus
) {
    public ProviderCapabilities {
        pix = false;
    }

    public static ProviderCapabilities pending() {
        return new ProviderCapabilities(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                PaymentProviderImplementationStatus.IMPLEMENTATION_PENDING
        );
    }

    public static ProviderCapabilities simulator() {
        return new ProviderCapabilities(
                true,
                true,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                PaymentProviderImplementationStatus.AVAILABLE
        );
    }

    public static ProviderCapabilities pagBankLocal() {
        return new ProviderCapabilities(
                true,
                true,
                false,
                false,
                false,
                true,
                false,
                true,
                false,
                PaymentProviderImplementationStatus.AVAILABLE
        );
    }
}
