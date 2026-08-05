package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.entity.CheckoutStatus;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutFinalizationService {

    private final CheckoutSessionRepository checkoutRepository;
    private final List<CheckoutFinalizationHandler> handlers;
    private final List<CheckoutFinalizationSideEffect> sideEffects;
    private final Clock clock;

    public CheckoutFinalizationService(
            CheckoutSessionRepository checkoutRepository,
            List<CheckoutFinalizationHandler> handlers,
            List<CheckoutFinalizationSideEffect> sideEffects,
            Clock clock
    ) {
        this.checkoutRepository = checkoutRepository;
        this.handlers = List.copyOf(handlers);
        this.sideEffects = List.copyOf(sideEffects);
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FinalizationResult finalizeCheckoutIfSupported(
            UUID checkoutId,
            Long actorUserId
    ) {
        return finalizeCheckout(
                checkoutId,
                actorUserId,
                false
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FinalizationResult finalizeCheckoutRequired(
            UUID checkoutId,
            Long actorUserId
    ) {
        return finalizeCheckout(
                checkoutId,
                actorUserId,
                true
        );
    }

    private FinalizationResult finalizeCheckout(
            UUID checkoutId,
            Long actorUserId,
            boolean handlerRequired
    ) {
        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findByIdForUpdate(checkoutId)
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        CheckoutFinalizationHandler handler =
                findHandler(checkout);

        if (handler == null) {
            if (handlerRequired) {
                throw new BusinessException(
                        "CHECKOUT_FINALIZATION_NOT_AVAILABLE",
                        "A finalizacao desta operacao ainda nao esta disponivel."
                );
            }

            return new FinalizationResult(
                    CheckoutSessionResponse.from(checkout),
                    false,
                    false,
                    0
            );
        }

        if (checkout.getStatus()
                == CheckoutStatus.FINALIZED) {

            OffsetDateTime finalizedAt =
                    checkout.getFinalizedAt() != null
                            ? checkout.getFinalizedAt()
                            : OffsetDateTime.now(clock);

            runSideEffects(
                    checkout,
                    actorUserId,
                    finalizedAt
            );

            return new FinalizationResult(
                    CheckoutSessionResponse.from(checkout),
                    true,
                    false,
                    0
            );
        }

        if (checkout.getStatus()
                != CheckoutStatus.PAID) {

            throw new BusinessException(
                    "CHECKOUT_NOT_PAID",
                    "O checkout precisa estar pago antes da finalizacao."
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(clock);

        int processedItems =
                handler.finalizeOperation(
                        checkout,
                        actorUserId,
                        now
                );

        checkout.finalizeOnce(now);
        checkoutRepository.flush();

        runSideEffects(
                checkout,
                actorUserId,
                now
        );

        return new FinalizationResult(
                CheckoutSessionResponse.from(checkout),
                true,
                true,
                processedItems
        );
    }

    private CheckoutFinalizationHandler findHandler(
            CheckoutSessionEntity checkout
    ) {
        return handlers.stream()
                .filter(handler ->
                        handler.supports(
                                checkout.getOperationType()
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private void runSideEffects(
            CheckoutSessionEntity checkout,
            Long actorUserId,
            OffsetDateTime finalizedAt
    ) {
        for (CheckoutFinalizationSideEffect sideEffect : sideEffects) {
            sideEffect.afterCheckoutFinalized(
                    checkout,
                    actorUserId,
                    finalizedAt
            );
        }
    }

    public record FinalizationResult(
            CheckoutSessionResponse checkout,
            boolean supported,
            boolean finalizedNow,
            int processedItems
    ) {
    }
}
