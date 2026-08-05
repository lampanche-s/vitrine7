package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.service.CheckoutFinalizationHandler;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class LavaWorkOrderFinalizationHandler
        implements CheckoutFinalizationHandler {

    private final LavaWorkOrderRepository workOrderRepository;

    @Override
    public boolean supports(
            CheckoutOperationType operationType
    ) {
        return operationType
                == CheckoutOperationType.LAVA_WORK_ORDER;
    }

    @Override
    public int finalizeOperation(
            CheckoutSessionEntity checkout,
            Long actorUserId,
            OffsetDateTime finalizedAt
    ) {
        LavaWorkOrderEntity workOrder =
                workOrderRepository
                        .findByCheckoutSessionIdForUpdate(
                                checkout.getId()
                        )
                        .orElseThrow(() -> new NotFoundException(
                                "LAVA_WORK_ORDER_NOT_FOUND",
                                "Ordem de servico nao encontrada."
                        ));

        validateCheckoutLink(
                workOrder,
                checkout
        );

        boolean paidNow =
                workOrder.markPaid(
                        checkout.getId(),
                        actorUserId,
                        finalizedAt
                );

        workOrderRepository.flush();

        return paidNow ? 1 : 0;
    }

    private void validateCheckoutLink(
            LavaWorkOrderEntity workOrder,
            CheckoutSessionEntity checkout
    ) {
        if (checkout.getOperationType()
                != CheckoutOperationType.LAVA_WORK_ORDER
                || checkout.getSourceId() == null
                || !checkout.getSourceId()
                .equals(workOrder.getId())
                || workOrder.getCheckoutSessionId() == null
                || !workOrder.getCheckoutSessionId()
                .equals(checkout.getId())) {

            throw new BusinessException(
                    "LAVA_WORK_ORDER_CHECKOUT_LINK_INVALID",
                    "O vinculo entre a ordem de servico e o checkout e invalido."
            );
        }

        if (workOrder.getStatus()
                != LavaWorkOrderStatus.PAYMENT_PENDING) {

            throw new BusinessException(
                    "LAVA_WORK_ORDER_NOT_PAYMENT_PENDING",
                    "A ordem de servico nao esta aguardando pagamento."
            );
        }

        if (!workOrder.getSubtotalCents()
                .equals(checkout.getSubtotalCents())
                || !workOrder.getDiscountCents()
                .equals(checkout.getDiscountCents())
                || !workOrder.getTotalCents()
                .equals(checkout.getTotalCents())) {

            throw new BusinessException(
                    "LAVA_WORK_ORDER_CHECKOUT_AMOUNT_MISMATCH",
                    "Os valores da ordem de servico nao conferem com o checkout."
            );
        }
    }
}
