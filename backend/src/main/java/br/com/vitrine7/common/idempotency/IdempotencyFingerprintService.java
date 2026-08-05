package br.com.vitrine7.common.idempotency;

import br.com.vitrine7.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class IdempotencyFingerprintService {

    public String sha256(String canonicalPayload) {
        if (canonicalPayload == null
                || canonicalPayload.isBlank()) {

            throw new BusinessException(
                    "INVALID_IDEMPOTENCY_PAYLOAD",
                    "Nao foi possivel identificar o conteudo idempotente da requisicao."
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    canonicalPayload.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 nao esta disponivel.",
                    exception
            );
        }
    }
}
