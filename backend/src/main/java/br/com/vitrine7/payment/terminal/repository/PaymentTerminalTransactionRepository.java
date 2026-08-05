package br.com.vitrine7.payment.terminal.repository;

import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTerminalTransactionRepository
        extends JpaRepository<PaymentTerminalTransactionEntity, UUID> {

    Optional<PaymentTerminalTransactionEntity> findByIdempotencyKey(
            UUID idempotencyKey
    );

    Optional<PaymentTerminalTransactionEntity> findByPaymentId(
            UUID paymentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT transaction
            FROM PaymentTerminalTransactionEntity transaction
            WHERE transaction.paymentId = :paymentId
            """)
    Optional<PaymentTerminalTransactionEntity> findByPaymentIdForUpdate(
            @Param("paymentId") UUID paymentId
    );

    List<PaymentTerminalTransactionEntity>
    findAllByCheckoutSessionIdOrderByCreatedAtAsc(
            UUID checkoutSessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT transaction
            FROM PaymentTerminalTransactionEntity transaction
            WHERE transaction.id = :id
            """)
    Optional<PaymentTerminalTransactionEntity>
    findByIdForUpdate(
            @Param("id") UUID id
    );
}
