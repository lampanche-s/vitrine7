package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class CheckoutResourceReleaseCoordinator {

    private final List<CheckoutResourceReleaseHandler> handlers;

    public CheckoutResourceReleaseCoordinator(
            List<CheckoutResourceReleaseHandler> handlers
    ) {
        this.handlers = List.copyOf(handlers);
    }

    public void release(
            CheckoutSessionEntity checkout,
            String reason,
            OffsetDateTime releasedAt
    ) {
        handlers.stream()
                .filter(handler ->
                        handler.supports(
                                checkout.getOperationType()
                        )
                )
                .forEach(handler ->
                        handler.release(
                                checkout.getId(),
                                reason,
                                releasedAt
                        )
                );
    }
}
