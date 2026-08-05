package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutSessionCreationService {

    private final CheckoutSessionRepository checkoutRepository;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public CheckoutSessionEntity createNew(
            UUID idempotencyKey,
            String requestFingerprint,
            CheckoutOperationType operationType,
            Long createdByUserId,
            OffsetDateTime expiresAt
    ) {
        CheckoutSessionEntity checkout =
                CheckoutSessionEntity.openDraft(
                        idempotencyKey,
                        requestFingerprint,
                        operationType,
                        createdByUserId,
                        expiresAt
                );

        return checkoutRepository.saveAndFlush(checkout);
    }
}
