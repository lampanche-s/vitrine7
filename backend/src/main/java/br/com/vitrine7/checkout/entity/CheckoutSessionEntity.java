package br.com.vitrine7.checkout.entity;

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
@Table(name = "checkout_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckoutSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true
    )
    private UUID idempotencyKey;

    @Column(
            name = "request_fingerprint",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "business_area",
            nullable = false,
            length = 20
    )
    private CheckoutBusinessArea businessArea;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "operation_type",
            nullable = false,
            length = 30
    )
    private CheckoutOperationType operationType;

    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private CheckoutStatus status;

    @Column(
            name = "subtotal_cents",
            nullable = false
    )
    private Long subtotalCents;

    @Column(
            name = "discount_cents",
            nullable = false
    )
    private Long discountCents;

    @Column(
            name = "total_cents",
            nullable = false
    )
    private Long totalCents;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            length = 30
    )
    private CheckoutDocumentType documentType;

    @Column(
            name = "cpf_digits",
            length = 11
    )
    private String cpfDigits;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Column(
            name = "cancel_reason",
            length = 255
    )
    private String cancelReason;

    @Column(
            name = "created_by_user_id",
            nullable = false
    )
    private Long createdByUserId;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static CheckoutSessionEntity openDraft(
            UUID idempotencyKey,
            String requestFingerprint,
            CheckoutOperationType operationType,
            Long createdByUserId,
            OffsetDateTime expiresAt
    ) {
        CheckoutSessionEntity checkout =
                new CheckoutSessionEntity();

        checkout.idempotencyKey = idempotencyKey;
        checkout.requestFingerprint =
                requestFingerprint;
        checkout.operationType = operationType;
        checkout.businessArea =
                switch (operationType) {
                    case BAR_COMMAND ->
                            CheckoutBusinessArea.BAR;
                    case LAVA_WORK_ORDER ->
                            CheckoutBusinessArea.LAVA;
                };
        checkout.sourceId = null;
        checkout.status = CheckoutStatus.DRAFT;
        checkout.subtotalCents = 0L;
        checkout.discountCents = 0L;
        checkout.totalCents = 0L;
        checkout.documentType = null;
        checkout.cpfDigits = null;
        checkout.expiresAt = expiresAt;
        checkout.paidAt = null;
        checkout.finalizedAt = null;
        checkout.cancelledAt = null;
        checkout.cancelledByUserId = null;
        checkout.cancelReason = null;
        checkout.createdByUserId = createdByUserId;

        return checkout;
    }

    public void attachSource(Long sourceId) {
        requireStatus(
                CheckoutStatus.DRAFT,
                "CHECKOUT_SOURCE_CANNOT_BE_CHANGED",
                "A operacao do checkout nao pode mais ser alterada."
        );

        if (sourceId == null || sourceId <= 0) {
            throw new BusinessException(
                    "INVALID_CHECKOUT_SOURCE",
                    "A referencia da operacao e invalida."
            );
        }

        if (this.sourceId != null
                && !this.sourceId.equals(sourceId)) {

            throw new BusinessException(
                    "CHECKOUT_SOURCE_ALREADY_ATTACHED",
                    "O checkout ja esta vinculado a outra operacao."
            );
        }

        this.sourceId = sourceId;
    }

    public void prepareForPayment(
            long subtotalCents,
            long discountCents,
            CheckoutDocumentType documentType,
            String cpfDigits,
            OffsetDateTime newExpiresAt
    ) {
        requireStatus(
                CheckoutStatus.DRAFT,
                "CHECKOUT_NOT_DRAFT",
                "Somente um checkout em rascunho pode ser preparado para pagamento."
        );

        validateAmounts(
                subtotalCents,
                discountCents
        );
        validateDocument(
                documentType,
                cpfDigits
        );

        this.subtotalCents = subtotalCents;
        this.discountCents = discountCents;
        this.totalCents =
                subtotalCents - discountCents;
        this.documentType = documentType;
        this.cpfDigits = cpfDigits;
        this.expiresAt = newExpiresAt;
        this.status =
                CheckoutStatus.READY_FOR_PAYMENT;
    }

    public void markPaymentProcessing() {
        if (status != CheckoutStatus.READY_FOR_PAYMENT
                && status != CheckoutStatus.PAYMENT_FAILED) {

            throw new BusinessException(
                    "CHECKOUT_NOT_READY_FOR_PAYMENT",
                    "O checkout nao esta pronto para iniciar o pagamento."
            );
        }

        this.status =
                CheckoutStatus.PAYMENT_PROCESSING;
    }

    public void markPaymentFailed() {
        requireStatus(
                CheckoutStatus.PAYMENT_PROCESSING,
                "CHECKOUT_NOT_PROCESSING_PAYMENT",
                "O checkout nao possui pagamento em processamento."
        );

        this.status =
                CheckoutStatus.PAYMENT_FAILED;
    }

    public void markPaid(OffsetDateTime paidAt) {
        if (status == CheckoutStatus.PAID
                || status == CheckoutStatus.FINALIZED) {
            return;
        }

        if (status != CheckoutStatus.PAYMENT_PROCESSING
                && status != CheckoutStatus.READY_FOR_PAYMENT) {

            throw new BusinessException(
                    "CHECKOUT_CANNOT_BE_MARKED_PAID",
                    "O checkout nao esta em um estado que permita confirmar o pagamento."
            );
        }

        this.status = CheckoutStatus.PAID;
        this.paidAt = paidAt;
    }

    public boolean finalizeOnce(
            OffsetDateTime finalizedAt
    ) {
        if (status == CheckoutStatus.FINALIZED) {
            return false;
        }

        requireStatus(
                CheckoutStatus.PAID,
                "CHECKOUT_NOT_PAID",
                "O checkout precisa estar pago antes da finalizacao."
        );

        this.status = CheckoutStatus.FINALIZED;
        this.finalizedAt = finalizedAt;

        return true;
    }

    public boolean cancelBeforePayment(
            Long actorUserId,
            String reason,
            OffsetDateTime cancelledAt
    ) {
        if (status == CheckoutStatus.CANCELLED) {
            return false;
        }

        if (!status.canCancelBeforePayment()) {
            throw new BusinessException(
                    "CHECKOUT_CANNOT_BE_CANCELLED",
                    "Este checkout nao pode ser cancelado pelo fluxo de cancelamento pre-pagamento."
            );
        }

        String normalizedReason =
                normalizeCancelReason(reason);

        this.status = CheckoutStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.cancelledByUserId = actorUserId;
        this.cancelReason = normalizedReason;

        return true;
    }

    public boolean expireIfNecessary(
            OffsetDateTime now
    ) {
        if (!status.canExpire()) {
            return false;
        }

        if (expiresAt.isAfter(now)) {
            return false;
        }

        this.status = CheckoutStatus.EXPIRED;

        return true;
    }

    private void validateAmounts(
            long subtotalCents,
            long discountCents
    ) {
        if (subtotalCents < 0) {
            throw new BusinessException(
                    "INVALID_CHECKOUT_SUBTOTAL",
                    "O subtotal do checkout e invalido."
            );
        }

        if (discountCents < 0
                || discountCents > subtotalCents) {

            throw new BusinessException(
                    "INVALID_CHECKOUT_DISCOUNT",
                    "O desconto do checkout e invalido."
            );
        }
    }

    private void validateDocument(
            CheckoutDocumentType documentType,
            String cpfDigits
    ) {
        if (documentType != CheckoutDocumentType.GENERAL_RECEIPT) {
            throw new BusinessException(
                    "INVALID_CHECKOUT_DOCUMENT",
                    "O checkout deve utilizar recibo geral."
            );
        }

        if (cpfDigits != null) {
            throw new BusinessException(
                    "CHECKOUT_CPF_NOT_ALLOWED",
                    "CPF nao e aceito neste fluxo."
            );
        }
    }

    private String normalizeCancelReason(String reason) {
        if (reason == null) {
            throw invalidCancelReason();
        }

        String normalized = reason
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.length() < 3
                || normalized.length() > 255) {

            throw invalidCancelReason();
        }

        return normalized;
    }

    private BusinessException invalidCancelReason() {
        return new BusinessException(
                "INVALID_CHECKOUT_CANCEL_REASON",
                "O motivo do cancelamento deve possuir entre 3 e 255 caracteres."
        );
    }

    private void requireStatus(
            CheckoutStatus expectedStatus,
            String code,
            String message
    ) {
        if (status != expectedStatus) {
            throw new BusinessException(
                    code,
                    message
            );
        }
    }
}
