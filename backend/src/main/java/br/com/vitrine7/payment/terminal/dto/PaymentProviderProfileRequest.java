package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PaymentProviderProfileRequest(
        @NotNull(message = "Informe o provider.")
        PaymentProviderCode providerCode,

        @NotBlank(message = "Informe o nome de exibicao.")
        @Size(max = 120, message = "Nome de exibicao deve ter no maximo 120 caracteres.")
        String displayName,

        @NotNull(message = "Informe o ambiente.")
        PaymentProviderEnvironment environment,

        @NotNull(message = "Informe se o perfil esta habilitado.")
        Boolean enabled,

        Boolean active,

        @Size(max = 120, message = "Referencia do merchant deve ter no maximo 120 caracteres.")
        String merchantReference,

        @Size(max = 120, message = "Referencia do terminal deve ter no maximo 120 caracteres.")
        String terminalReference,

        Map<String, Object> publicConfiguration,

        Map<String, String> credentials,

        Long version
) {
    @Override
    public String toString() {
        return "PaymentProviderProfileRequest[providerCode="
                + providerCode
                + ", displayName="
                + displayName
                + ", environment="
                + environment
                + ", enabled="
                + enabled
                + ", active="
                + active
                + ", merchantReference="
                + merchantReference
                + ", terminalReference="
                + terminalReference
                + ", publicConfiguration="
                + publicConfiguration
                + ", credentials=[omitted], version="
                + version
                + "]";
    }
}
