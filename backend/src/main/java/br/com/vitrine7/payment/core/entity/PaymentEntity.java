package br.com.vitrine7.payment.core.entity;

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
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "checkout_session_id", nullable = false)
    private UUID checkoutSessionId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode", nullable = false, length = 30)
    private PaymentProcessingMode processingMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "cash_received_cents")
    private Long cashReceivedCents;

    @Column(name = "cash_change_cents")
    private Long cashChangeCents;

    @Column(name = "cash_confirmed_by_user_id")
    private Long cashConfirmedByUserId;

    @Column(name = "manual_reason", length = 255)
    private String manualReason;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "reversed_at")
    private OffsetDateTime reversedAt;

    @Column(name = "reversed_by_user_id")
    private Long reversedByUserId;

    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

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

    public static PaymentEntity approvedCash(
            UUID checkoutSessionId,
            UUID idempotencyKey,
            String requestFingerprint,
            long amountCents,
            long cashReceivedCents,
            Long actorUserId,
            OffsetDateTime approvedAt
    ) {
        PaymentEntity payment = baseApproved(
                checkoutSessionId,
                idempotencyKey,
                requestFingerprint,
                PaymentMethod.CASH,
                PaymentProcessingMode.CASH,
                amountCents,
                actorUserId,
                approvedAt
        );

        payment.cashReceivedCents = cashReceivedCents;
        payment.cashChangeCents = cashReceivedCents - amountCents;
        payment.cashConfirmedByUserId = actorUserId;
        payment.manualReason = null;

        return payment;
    }

    public static PaymentEntity approvedManualFallback(
            UUID checkoutSessionId,
            UUID idempotencyKey,
            String requestFingerprint,
            PaymentMethod method,
            long amountCents,
            String manualReason,
            Long actorUserId,
            OffsetDateTime approvedAt
    ) {
        PaymentEntity payment = baseApproved(
                checkoutSessionId,
                idempotencyKey,
                requestFingerprint,
                method,
                PaymentProcessingMode.MANUAL_FALLBACK,
                amountCents,
                actorUserId,
                approvedAt
        );

        payment.cashReceivedCents = null;
        payment.cashChangeCents = null;
        payment.cashConfirmedByUserId = null;
        payment.manualReason = manualReason;

        return payment;
    }

    public static PaymentEntity processingTerminal(
            UUID checkoutSessionId,
            UUID idempotencyKey,
            String requestFingerprint,
            PaymentMethod method,
            PaymentProcessingMode processingMode,
            long amountCents,
            Long actorUserId
    ) {
        PaymentEntity payment = new PaymentEntity();

        payment.checkoutSessionId = checkoutSessionId;
        payment.idempotencyKey = idempotencyKey;
        payment.requestFingerprint = requestFingerprint;
        payment.method = method;
        payment.processingMode = processingMode;
        payment.status = PaymentStatus.PROCESSING;
        payment.amountCents = amountCents;
        payment.createdByUserId = actorUserId;

        return payment;
    }

    public void markApproved(
            Long actorUserId,
            OffsetDateTime approvedAt
    ) {
        if (status == PaymentStatus.APPROVED) {
            return;
        }

        if (status != PaymentStatus.PROCESSING) {
            throw new BusinessException(
                    "PAYMENT_NOT_PROCESSING",
                    "O pagamento nao esta em processamento."
            );
        }

        this.status = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt;
        this.approvedByUserId = actorUserId;
        this.declinedAt = null;
    }

    public void markDeclined(
            OffsetDateTime declinedAt
    ) {
        if (status == PaymentStatus.DECLINED) {
            return;
        }

        if (status != PaymentStatus.PROCESSING) {
            throw new BusinessException(
                    "PAYMENT_NOT_PROCESSING",
                    "O pagamento nao esta em processamento."
            );
        }

        this.status = PaymentStatus.DECLINED;
        this.declinedAt = declinedAt;
        this.approvedAt = null;
        this.approvedByUserId = null;
    }

    public boolean markReversalPending() {
        if (status == PaymentStatus.REVERSAL_PENDING) {
            return false;
        }

        if (status != PaymentStatus.APPROVED) {
            throw new BusinessException(
                    "PAYMENT_CANNOT_BE_REVERSED",
                    "Somente um pagamento aprovado pode iniciar estorno."
            );
        }

        this.status = PaymentStatus.REVERSAL_PENDING;
        this.reversedAt = null;
        this.reversedByUserId = null;
        this.reversalReason = null;

        return true;
    }

    public boolean restoreApprovedAfterReversalFailure() {
        if (status == PaymentStatus.APPROVED) {
            return false;
        }

        if (status != PaymentStatus.REVERSAL_PENDING) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_NOT_PENDING",
                    "O pagamento nao possui estorno pendente."
            );
        }

        this.status = PaymentStatus.APPROVED;
        this.reversedAt = null;
        this.reversedByUserId = null;
        this.reversalReason = null;

        return true;
    }

    public boolean markReversed(
            Long actorUserId,
            String reason,
            OffsetDateTime reversedAt
    ) {
        if (status == PaymentStatus.REVERSED) {
            return false;
        }

        if (status != PaymentStatus.REVERSAL_PENDING) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_NOT_PENDING",
                    "O pagamento nao possui estorno pendente."
            );
        }

        String normalizedReason =
                normalizeReversalReason(reason);

        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_ACTOR_REQUIRED",
                    "O usuario responsavel pelo estorno e obrigatorio."
            );
        }

        if (reversedAt == null) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_DATE_REQUIRED",
                    "A data do estorno e obrigatoria."
            );
        }

        this.status = PaymentStatus.REVERSED;
        this.reversedAt = reversedAt;
        this.reversedByUserId = actorUserId;
        this.reversalReason = normalizedReason;

        return true;
    }

    public static String normalizeReversalReason(
            String reason
    ) {
        if (reason == null) {
            throw invalidReversalReason();
        }

        String normalized =
                reason.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalized.length() < 3
                || normalized.length() > 255) {

            throw invalidReversalReason();
        }

        return normalized;
    }

    private static BusinessException invalidReversalReason() {
        return new BusinessException(
                "INVALID_PAYMENT_REVERSAL_REASON",
                "O motivo do estorno deve possuir entre 3 e 255 caracteres."
        );
    }

    private static PaymentEntity baseApproved(
            UUID checkoutSessionId,
            UUID idempotencyKey,
            String requestFingerprint,
            PaymentMethod method,
            PaymentProcessingMode processingMode,
            long amountCents,
            Long actorUserId,
            OffsetDateTime approvedAt
    ) {
        PaymentEntity payment = new PaymentEntity();

        payment.checkoutSessionId = checkoutSessionId;
        payment.idempotencyKey = idempotencyKey;
        payment.requestFingerprint = requestFingerprint;
        payment.method = method;
        payment.processingMode = processingMode;
        payment.status = PaymentStatus.APPROVED;
        payment.amountCents = amountCents;
        payment.approvedAt = approvedAt;
        payment.approvedByUserId = actorUserId;
        payment.createdByUserId = actorUserId;
        payment.declinedAt = null;
        payment.cancelledAt = null;
        payment.cancelledByUserId = null;
        payment.cancelReason = null;
        payment.reversedAt = null;
        payment.reversedByUserId = null;
        payment.reversalReason = null;

        return payment;
    }
}
