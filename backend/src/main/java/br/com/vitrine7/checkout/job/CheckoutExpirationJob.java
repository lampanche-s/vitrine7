package br.com.vitrine7.checkout.job;

import br.com.vitrine7.checkout.service.CheckoutExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutExpirationJob {

    private final CheckoutExpirationService expirationService;

    @Scheduled(
            fixedDelayString =
                    "${app.checkout.expiration-scan-delay-ms:60000}"
    )
    public void expireStaleSessions() {
        int expired =
                expirationService.expireStaleSessions();

        if (expired > 0) {
            log.info(
                    "{} checkout(s) expirado(s) automaticamente.",
                    expired
            );
        }
    }
}
