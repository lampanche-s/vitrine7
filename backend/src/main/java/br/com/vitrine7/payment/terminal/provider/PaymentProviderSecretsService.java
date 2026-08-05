package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class PaymentProviderSecretsService {

    public static final int CURRENT_KEY_VERSION = 1;
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String configuredKey;

    public PaymentProviderSecretsService(
            ObjectMapper objectMapper,
            @Value("${app.payment-providers.secrets-key:}")
            String configuredKey
    ) {
        this.objectMapper = objectMapper;
        this.configuredKey = configuredKey;
    }

    public EncryptedCredentials encrypt(
            Map<String, String> credentials
    ) {
        if (credentials == null || credentials.isEmpty()) {
            return null;
        }

        byte[] key = loadKey();
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce)
            );

            byte[] plaintext =
                    objectMapper.writeValueAsBytes(credentials);
            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptedCredentials(
                    ciphertext,
                    nonce,
                    CURRENT_KEY_VERSION
            );

        } catch (Exception exception) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_CREDENTIALS_INVALID",
                    "Nao foi possivel proteger as credenciais informadas."
            );
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    private byte[] loadKey() {
        String raw = configuredKey == null || configuredKey.isBlank()
                ? System.getenv("VITRINE7_PROVIDER_SECRETS_KEY")
                : configuredKey;

        if (raw == null || raw.isBlank()) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_SECRETS_KEY_NOT_CONFIGURED",
                    "A chave mestra de credenciais de providers nao esta configurada."
            );
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(raw);
            if (decoded.length != 32) {
                throw new IllegalArgumentException("invalid size");
            }
            return decoded;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_SECRETS_KEY_NOT_CONFIGURED",
                    "A chave mestra de credenciais de providers deve ter 32 bytes em Base64."
            );
        }
    }

    public record EncryptedCredentials(
            byte[] ciphertext,
            byte[] nonce,
            int keyVersion
    ) {
    }
}
