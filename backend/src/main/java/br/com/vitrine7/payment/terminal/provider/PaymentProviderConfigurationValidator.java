package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PaymentProviderConfigurationValidator {

    private static final int MAX_PUBLIC_KEYS = 30;
    private static final int MAX_CREDENTIAL_KEYS = 20;
    private static final int MAX_VALUE_LENGTH = 2048;
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,63}$");
    private static final Set<String> SENSITIVE_FRAGMENTS = Set.of(
            "password",
            "secret",
            "token",
            "apikey",
            "api_key",
            "privatekey",
            "private_key",
            "certificate",
            "certificado",
            "credential",
            "authorization",
            "bearer",
            "card",
            "cvv",
            "pan"
    );

    public Map<String, Object> validatePublicConfiguration(
            Map<String, Object> configuration
    ) {
        Map<String, Object> safe =
                configuration == null ? Map.of() : configuration;

        if (safe.size() > MAX_PUBLIC_KEYS) {
            throw invalid("Configuracao publica possui chaves demais.");
        }

        safe.forEach((key, value) -> {
            validateKey(key);
            if (isSensitiveKey(key)) {
                throw invalid("Configuracao publica contem chave sensivel.");
            }
            validatePublicValue(value);
        });

        return safe;
    }

    public Map<String, String> validateCredentials(
            Map<String, String> credentials
    ) {
        if (credentials == null || credentials.isEmpty()) {
            return Map.of();
        }

        if (credentials.size() > MAX_CREDENTIAL_KEYS) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_CREDENTIALS_INVALID",
                    "Credenciais possuem chaves demais."
            );
        }

        credentials.forEach((key, value) -> {
            validateKey(key);
            if (value == null
                    || value.isBlank()
                    || value.length() > MAX_VALUE_LENGTH) {
                throw new BusinessException(
                        "PAYMENT_PROVIDER_CREDENTIALS_INVALID",
                        "Credenciais possuem valor invalido."
                );
            }
        });

        return credentials;
    }

    private void validateKey(String key) {
        if (key == null
                || !KEY_PATTERN.matcher(key).matches()) {
            throw invalid("Configuracao possui chave invalida.");
        }
    }

    @SuppressWarnings("unchecked")
    private void validatePublicValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            if (value instanceof String stringValue
                    && stringValue.length() > MAX_VALUE_LENGTH) {
                throw invalid("Configuracao publica possui valor muito longo.");
            }
            return;
        }

        if (value instanceof Map<?, ?> mapValue) {
            if (mapValue.size() > MAX_PUBLIC_KEYS) {
                throw invalid("Configuracao publica possui objeto muito grande.");
            }
            ((Map<String, Object>) mapValue).forEach((key, nestedValue) -> {
                validateKey(key);
                if (isSensitiveKey(key)) {
                    throw invalid("Configuracao publica contem chave sensivel.");
                }
                validatePublicValue(nestedValue);
            });
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            int count = 0;
            for (Object item : iterable) {
                count++;
                if (count > MAX_PUBLIC_KEYS) {
                    throw invalid("Configuracao publica possui lista muito grande.");
                }
                validatePublicValue(item);
            }
            return;
        }

        throw invalid("Configuracao publica possui valor invalido.");
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key
                .replace("-", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);

        return SENSITIVE_FRAGMENTS.stream()
                .anyMatch(normalized::contains);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(
                "PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                message
        );
    }
}
