package br.com.vitrine7.pagbankagent;

import java.util.UUID;

public record DeviceToken(
        UUID deviceId,
        String token
) {
}
