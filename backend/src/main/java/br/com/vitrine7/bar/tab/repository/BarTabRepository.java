package br.com.vitrine7.bar.tab.repository;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BarTabRepository extends
        JpaRepository<BarTabEntity, Long>,
        JpaSpecificationExecutor<BarTabEntity> {

    Optional<BarTabEntity>
    findByCreateIdempotencyKey(UUID idempotencyKey);

    Optional<BarTabEntity>
    findByCheckoutSessionId(UUID checkoutSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT tab
            FROM BarTabEntity tab
            WHERE tab.id = :id
            """)
    Optional<BarTabEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT tab
            FROM BarTabEntity tab
            WHERE tab.checkoutSessionId = :checkoutId
            """)
    Optional<BarTabEntity> findByCheckoutSessionIdForUpdate(
            @Param("checkoutId") UUID checkoutId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    UPDATE bar_tabs tab
                    SET
                        status = 'OPEN',
                        checkout_session_id = NULL,
                        discount_cents = 0,
                        total_cents = tab.subtotal_cents,
                        prepare_idempotency_key = NULL,
                        prepare_request_fingerprint = NULL,
                        prepared_at = NULL,
                        updated_at = CURRENT_TIMESTAMP,
                        version = tab.version + 1
                    FROM checkout_sessions checkout
                    WHERE tab.checkout_session_id = checkout.id
                      AND tab.status = 'PAYMENT_PENDING'
                      AND checkout.status IN ('CANCELLED', 'EXPIRED')
                    """,
            nativeQuery = true
    )
    int reopenReleasedOrExpiredTabs();
}
