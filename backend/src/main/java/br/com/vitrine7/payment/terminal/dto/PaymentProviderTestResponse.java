package br.com.vitrine7.payment.terminal.dto;

public record PaymentProviderTestResponse(
        String providerCode,
        String implementationStatus,
        boolean success,
        String code,
        String message
) {
}
