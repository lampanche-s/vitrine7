package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.config.CheckoutProperties;
import br.com.vitrine7.checkout.dto.CancelCheckoutSessionRequest;
import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;
import br.com.vitrine7.checkout.dto.CreateCheckoutSessionRequest;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.entity.CheckoutStatus;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutSessionService {

    private static final String FINGERPRINT_VERSION =
            "checkout-create-v1";

    private final CheckoutSessionRepository checkoutRepository;
    private final CheckoutSessionCreationService creationService;
    private final IdempotencyFingerprintService fingerprintService;
    private final CheckoutProperties checkoutProperties;
    private final CheckoutResourceReleaseCoordinator releaseCoordinator;
    private final Clock clock;

    public OpenCheckoutResult open(
            UUID idempotencyKey,
            CreateCheckoutSessionRequest request,
            VitrineUserPrincipal principal
    ) {
        String fingerprint =
                createFingerprint(request);

        CheckoutSessionEntity existing =
                checkoutRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existing != null) {
            validateIdempotentReplay(
                    existing,
                    fingerprint,
                    principal.getId()
            );

            return new OpenCheckoutResult(
                    CheckoutSessionResponse.from(existing),
                    true
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(clock);

        OffsetDateTime expiresAt = now.plus(
                checkoutProperties.getDraftExpiration()
        );

        try {
            CheckoutSessionEntity created =
                    creationService.createNew(
                            idempotencyKey,
                            fingerprint,
                            request.operationType(),
                            principal.getId(),
                            expiresAt
                    );

            return new OpenCheckoutResult(
                    CheckoutSessionResponse.from(created),
                    false
            );

        } catch (DataIntegrityViolationException exception) {
            CheckoutSessionEntity concurrentlyCreated =
                    checkoutRepository
                            .findByIdempotencyKey(
                                    idempotencyKey
                            )
                            .orElseThrow(() -> exception);

            validateIdempotentReplay(
                    concurrentlyCreated,
                    fingerprint,
                    principal.getId()
            );

            return new OpenCheckoutResult(
                    CheckoutSessionResponse.from(
                            concurrentlyCreated
                    ),
                    true
            );
        }
    }

    @Transactional
    public CheckoutSessionResponse findById(UUID id) {
        CheckoutSessionEntity checkout =
                getForUpdate(id);

        checkout.expireIfNecessary(
                OffsetDateTime.now(clock)
        );

        return CheckoutSessionResponse.from(checkout);
    }

    @Transactional
    public CheckoutSessionResponse cancel(
            UUID id,
            CancelCheckoutSessionRequest request,
            VitrineUserPrincipal principal
    ) {
        CheckoutSessionEntity checkout =
                getForUpdate(id);

        OffsetDateTime now = OffsetDateTime.now(clock);

        checkout.expireIfNecessary(now);

        if (checkout.getStatus()
                == CheckoutStatus.EXPIRED) {

            releaseCoordinator.release(
                    checkout,
                    "Checkout expirado",
                    now
            );

            return CheckoutSessionResponse.from(checkout);
        }

        checkout.cancelBeforePayment(
                principal.getId(),
                request.reason(),
                now
        );

        releaseCoordinator.release(
                checkout,
                request.reason(),
                now
        );


        return CheckoutSessionResponse.from(checkout);
    }

    private CheckoutSessionEntity getForUpdate(UUID id) {
        return checkoutRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(
                        "CHECKOUT_NOT_FOUND",
                        "Checkout nao encontrado."
                ));
    }

    private String createFingerprint(
            CreateCheckoutSessionRequest request
    ) {
        String canonicalPayload =
                FINGERPRINT_VERSION
                        + "|operationType="
                        + request.operationType().name();

        return fingerprintService.sha256(
                canonicalPayload
        );
    }

    private void validateIdempotentReplay(
            CheckoutSessionEntity existing,
            String requestFingerprint,
            Long actorUserId
    ) {
        if (!existing.getCreatedByUserId()
                .equals(actorUserId)) {

            throw new BusinessException(
                    "IDEMPOTENCY_KEY_ALREADY_USED",
                    "A chave de idempotencia ja foi utilizada."
            );
        }

        if (!existing.getRequestFingerprint()
                .equals(requestFingerprint)) {

            throw new BusinessException(
                    "IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                    "A chave de idempotencia foi reutilizada com uma requisicao diferente."
            );
        }
    }

    public record OpenCheckoutResult(
            CheckoutSessionResponse checkout,
            boolean replayed
    ) {
    }
}
