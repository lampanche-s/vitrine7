package br.com.vitrine7.checkout.repository;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutSessionRepository
        extends JpaRepository<CheckoutSessionEntity, UUID> {

    Optional<CheckoutSessionEntity>
    findByIdempotencyKey(UUID idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT checkout
            FROM CheckoutSessionEntity checkout
            WHERE checkout.id = :id
            """)
    Optional<CheckoutSessionEntity> findByIdForUpdate(
            @Param("id") UUID id
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            UPDATE CheckoutSessionEntity checkout
            SET checkout.status =
                    br.com.vitrine7.checkout.entity.CheckoutStatus.EXPIRED,
                checkout.version = checkout.version + 1
            WHERE checkout.status IN (
                    br.com.vitrine7.checkout.entity.CheckoutStatus.DRAFT,
                    br.com.vitrine7.checkout.entity.CheckoutStatus.READY_FOR_PAYMENT,
                    br.com.vitrine7.checkout.entity.CheckoutStatus.PAYMENT_FAILED
            )
              AND checkout.expiresAt <= :now
              AND NOT EXISTS (
                    SELECT payment.id
                    FROM br.com.vitrine7.payment.core.entity.PaymentEntity payment
                    WHERE payment.checkoutSessionId = checkout.id
                      AND payment.status = br.com.vitrine7.payment.core.entity.PaymentStatus.APPROVED
              )
            """)
    int expireStaleSessions(
            @Param("now") OffsetDateTime now
    );
}
