package br.com.vitrine7.payment.terminal.bridge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerminalCommandExpirationJob {
    private final TerminalCommandExpirationService service;

    @Scheduled(fixedDelayString = "${app.payment-terminal.bridge.expiration-interval:5s}")
    public void expire() {
        try {
            service.expireBatch();
        } catch (RuntimeException exception) {
            log.error("Falha controlada ao expirar comandos de terminal.", exception);
        }
    }
}
