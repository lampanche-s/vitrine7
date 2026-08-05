package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.service.CheckoutResourceReleaseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BarTabCheckoutReleaseHandler
        implements CheckoutResourceReleaseHandler {

    private final BarTabRepository tabRepository;

    @Override
    public boolean supports(
            CheckoutOperationType operationType
    ) {
        return operationType
                == CheckoutOperationType.BAR_COMMAND;
    }

    @Override
    public void release(
            UUID checkoutId,
            String reason,
            OffsetDateTime releasedAt
    ) {
        BarTabEntity tab =
                tabRepository
                        .findByCheckoutSessionIdForUpdate(
                                checkoutId
                        )
                        .orElse(null);

        if (tab != null) {
            tab.reopenAfterCheckoutRelease(
                    checkoutId
            );
        }
    }
}
