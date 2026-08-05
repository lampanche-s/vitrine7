package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.service.CheckoutResourceReleaseHandler;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LavaWorkOrderCheckoutReleaseHandler
        implements CheckoutResourceReleaseHandler {

    private final LavaWorkOrderRepository workOrderRepository;

    @Override
    public boolean supports(CheckoutOperationType operationType) {
        return operationType
                == CheckoutOperationType.LAVA_WORK_ORDER;
    }

    @Override
    public void release(
            UUID checkoutId,
            String reason,
            OffsetDateTime releasedAt
    ) {
        LavaWorkOrderEntity workOrder =
                workOrderRepository
                        .findByCheckoutSessionIdForUpdate(checkoutId)
                        .orElse(null);

        if (workOrder != null) {
            workOrder.reopenAfterCheckoutRelease(checkoutId);
        }
    }
}
