package br.com.vitrine7.pagbankagent;

public record PaymentTerminalDriverSelection(
        PaymentTerminalDriver driver,
        PaymentTerminalCapabilities capabilities
) {
}
