package br.com.vitrine7.lava.workorder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LavaWorkOrderExpirationJob {

    private final LavaWorkOrderExpirationService expirationService;

    @Scheduled(
            fixedDelayString =
                    "${app.checkout.expiration-scan-delay-ms:60000}"
    )
    public void reopenExpiredWorkOrders() {
        int reopened =
                expirationService.reopenExpiredWorkOrders();

        if (reopened > 0) {
            log.info(
                    "{} ordem(ns) de servico reaberta(s) apos expiracao do checkout.",
                    reopened
            );
        }
    }
}
