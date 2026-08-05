package br.com.vitrine7.lava.workorder.repository;

import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LavaWorkOrderRepository extends
        JpaRepository<LavaWorkOrderEntity, Long>,
        JpaSpecificationExecutor<LavaWorkOrderEntity> {

    Optional<LavaWorkOrderEntity>
    findByCreateIdempotencyKey(UUID idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT workOrder
            FROM LavaWorkOrderEntity workOrder
            WHERE workOrder.id = :id
            """)
    Optional<LavaWorkOrderEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT workOrder
            FROM LavaWorkOrderEntity workOrder
            WHERE workOrder.checkoutSessionId = :checkoutId
            """)
    Optional<LavaWorkOrderEntity> findByCheckoutSessionIdForUpdate(
            @Param("checkoutId") UUID checkoutId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    UPDATE lava_work_orders work_order
                    SET
                        status = 'OPEN',
                        checkout_session_id = NULL,
                        discount_cents = 0,
                        total_cents = work_order.subtotal_cents,
                        prepare_idempotency_key = NULL,
                        prepare_request_fingerprint = NULL,
                        prepared_at = NULL,
                        updated_at = CURRENT_TIMESTAMP,
                        version = work_order.version + 1
                    FROM checkout_sessions checkout
                    WHERE work_order.checkout_session_id = checkout.id
                      AND work_order.status = 'PAYMENT_PENDING'
                      AND checkout.status IN ('CANCELLED', 'EXPIRED')
                    """,
            nativeQuery = true
    )
    int reopenReleasedOrExpiredWorkOrders();
}
