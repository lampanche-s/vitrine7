package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;

import java.util.UUID;

public record TerminalDevicePrincipal(UUID deviceId, PaymentProviderCode providerCode) {
}
