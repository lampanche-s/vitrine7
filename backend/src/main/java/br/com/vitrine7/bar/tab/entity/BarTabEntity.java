package br.com.vitrine7.bar.tab.entity;

import br.com.vitrine7.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bar_tabs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarTabEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BarTabStatus status;

    @Column(name = "checkout_session_id")
    private UUID checkoutSessionId;

    @Column(name = "subtotal_cents", nullable = false)
    private Long subtotalCents;

    @Column(name = "discount_cents", nullable = false)
    private Long discountCents;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents;

    @Column(name = "create_idempotency_key", nullable = false, unique = true)
    private UUID createIdempotencyKey;

    @Column(
            name = "create_request_fingerprint",
            nullable = false,
            columnDefinition = "CHAR(64)"
    )
    @JdbcTypeCode(SqlTypes.CHAR)
    private String createRequestFingerprint;

    @Column(name = "prepare_idempotency_key")
    private UUID prepareIdempotencyKey;

    @Column(name = "prepare_request_fingerprint", columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String prepareRequestFingerprint;

    @Column(name = "prepared_at")
    private OffsetDateTime preparedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static BarTabEntity open(
            String name,
            String normalizedName,
            UUID idempotencyKey,
            String requestFingerprint,
            Long actorUserId
    ) {
        BarTabEntity tab = new BarTabEntity();

        tab.name = name;
        tab.normalizedName = normalizedName;
        tab.status = BarTabStatus.OPEN;
        tab.subtotalCents = 0L;
        tab.discountCents = 0L;
        tab.totalCents = 0L;
        tab.createIdempotencyKey = idempotencyKey;
        tab.createRequestFingerprint = requestFingerprint;
        tab.createdByUserId = actorUserId;

        return tab;
    }

    public void rename(String name, String normalizedName) {
        requireOpen();

        this.name = name;
        this.normalizedName = normalizedName;
    }

    public void updateDraftTotal(long subtotalCents) {
        requireOpen();

        this.subtotalCents = subtotalCents;
        this.discountCents = 0L;
        this.totalCents = subtotalCents;
    }

    public void markPaymentPending(
            UUID checkoutSessionId,
            long subtotalCents,
            long discountCents,
            UUID idempotencyKey,
            String requestFingerprint,
            OffsetDateTime preparedAt
    ) {
        requireOpen();

        this.checkoutSessionId = checkoutSessionId;
        this.subtotalCents = subtotalCents;
        this.discountCents = discountCents;
        this.totalCents = subtotalCents - discountCents;
        this.prepareIdempotencyKey = idempotencyKey;
        this.prepareRequestFingerprint = requestFingerprint;
        this.preparedAt = preparedAt;
        this.status = BarTabStatus.PAYMENT_PENDING;
    }

    public void markClosed(
            UUID finalizedCheckoutId,
            OffsetDateTime closedAt
    ) {
        if (status == BarTabStatus.CLOSED) {
            return;
        }

        if (status != BarTabStatus.PAYMENT_PENDING
                || checkoutSessionId == null
                || !checkoutSessionId.equals(finalizedCheckoutId)) {

            throw new BusinessException(
                    "BAR_TAB_NOT_PAYMENT_PENDING",
                    "A comanda nao esta aguardando este pagamento."
            );
        }

        this.status = BarTabStatus.CLOSED;
        this.closedAt = closedAt;
    }

    public void cancelOpen(
            Long actorUserId,
            String reason,
            OffsetDateTime cancelledAt
    ) {
        if (status == BarTabStatus.CANCELLED) {
            return;
        }

        requireOpen();

        this.status = BarTabStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.cancelledByUserId = actorUserId;
        this.cancellationReason = reason;
    }

    public void reopenAfterCheckoutRelease(UUID releasedCheckoutId) {
        if (status != BarTabStatus.PAYMENT_PENDING) {
            return;
        }

        if (!releasedCheckoutId.equals(checkoutSessionId)) {
            return;
        }

        this.checkoutSessionId = null;
        this.discountCents = 0L;
        this.totalCents = subtotalCents;
        this.prepareIdempotencyKey = null;
        this.prepareRequestFingerprint = null;
        this.preparedAt = null;
        this.status = BarTabStatus.OPEN;
    }

    public boolean isPrepared() {
        return status == BarTabStatus.PAYMENT_PENDING && preparedAt != null;
    }

    public void requireOpen() {
        if (status != BarTabStatus.OPEN) {
            throw new BusinessException(
                    "BAR_TAB_NOT_OPEN",
                    "A comanda nao esta aberta para alteracoes."
            );
        }
    }
}
