package br.com.vitrine7.bar.tab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarTabExpirationJob {

    private final BarTabExpirationService expirationService;

    @Scheduled(
            fixedDelayString =
                    "${app.checkout.expiration-scan-delay-ms:60000}"
    )
    public void reopenExpiredTabs() {
        int reopened = expirationService.reopenExpiredTabs();

        if (reopened > 0) {
            log.info(
                    "{} comanda(s) reaberta(s) apos expiracao do checkout.",
                    reopened
            );
        }
    }
}
