package br.com.vitrine7.payment.core.repository;

import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdempotencyKey(
            UUID idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM PaymentEntity payment
            WHERE payment.id = :id
            """)
    Optional<PaymentEntity> findByIdForUpdate(
            @Param("id") UUID id
    );

    List<PaymentEntity>
    findAllByCheckoutSessionIdOrderByCreatedAtAsc(
            UUID checkoutSessionId
    );

    Optional<PaymentEntity>
    findFirstByCheckoutSessionIdAndStatusInOrderByCreatedAtDesc(
            UUID checkoutSessionId,
            Collection<PaymentStatus> statuses
    );
    @Query("""
            SELECT COALESCE(SUM(payment.amountCents), 0)
            FROM PaymentEntity payment
            WHERE payment.checkoutSessionId = :checkoutId
              AND payment.status = br.com.vitrine7.payment.core.entity.PaymentStatus.APPROVED
            """)
    long sumApprovedAmount(@Param("checkoutId") UUID checkoutId);

    @Query("""
            SELECT COUNT(payment)
            FROM PaymentEntity payment
            WHERE payment.checkoutSessionId = :checkoutId
              AND payment.status = br.com.vitrine7.payment.core.entity.PaymentStatus.APPROVED
            """)
    long countApprovedPayments(@Param("checkoutId") UUID checkoutId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM PaymentEntity payment
            WHERE payment.checkoutSessionId = :checkoutId
              AND payment.status = br.com.vitrine7.payment.core.entity.PaymentStatus.APPROVED
            ORDER BY payment.createdAt ASC
            """)
    List<PaymentEntity> findApprovedByCheckoutForUpdate(
            @Param("checkoutId") UUID checkoutId
    );
}
