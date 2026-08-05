package br.com.vitrine7.lava.workorder.entity;

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
@Table(name = "lava_work_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LavaWorkOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registered_client_id")
    private Long registeredClientId;

    @Column(name = "customer_name_snapshot", nullable = false, length = 120)
    private String customerNameSnapshot;

    @Column(
            name = "normalized_customer_name_snapshot",
            nullable = false,
            length = 120
    )
    private String normalizedCustomerNameSnapshot;

    @Column(name = "customer_phone_digits_snapshot", length = 11)
    private String customerPhoneDigitsSnapshot;

    @Column(name = "vehicle_name_snapshot", length = 120)
    private String vehicleNameSnapshot;

    @Column(name = "normalized_vehicle_name_snapshot", length = 120)
    private String normalizedVehicleNameSnapshot;

    @Column(name = "vehicle_plate_snapshot", length = 7)
    private String vehiclePlateSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_size", nullable = false, length = 20)
    private LavaVehicleSize vehicleSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LavaWorkOrderStatus status;

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

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "paid_by_user_id")
    private Long paidByUserId;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "completed_by_user_id")
    private Long completedByUserId;

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

    public static LavaWorkOrderEntity open(
            Long registeredClientId,
            String customerName,
            String normalizedCustomerName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate,
            LavaVehicleSize vehicleSize,
            UUID idempotencyKey,
            String requestFingerprint,
            Long actorUserId
    ) {
        LavaWorkOrderEntity order = new LavaWorkOrderEntity();

        order.registeredClientId = registeredClientId;
        order.customerNameSnapshot = customerName;
        order.normalizedCustomerNameSnapshot = normalizedCustomerName;
        order.customerPhoneDigitsSnapshot = phoneDigits;
        order.vehicleNameSnapshot = vehicleName;
        order.normalizedVehicleNameSnapshot = normalizedVehicleName;
        order.vehiclePlateSnapshot = plate;
        order.vehicleSize = vehicleSize;
        order.status = LavaWorkOrderStatus.OPEN;
        order.subtotalCents = 0L;
        order.discountCents = 0L;
        order.totalCents = 0L;
        order.createIdempotencyKey = idempotencyKey;
        order.createRequestFingerprint = requestFingerprint;
        order.createdByUserId = actorUserId;

        return order;
    }

    public void updateCustomerSnapshot(
            Long registeredClientId,
            String customerName,
            String normalizedCustomerName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate
    ) {
        requireOpen();

        this.registeredClientId = registeredClientId;
        this.customerNameSnapshot = customerName;
        this.normalizedCustomerNameSnapshot = normalizedCustomerName;
        this.customerPhoneDigitsSnapshot = phoneDigits;
        this.vehicleNameSnapshot = vehicleName;
        this.normalizedVehicleNameSnapshot = normalizedVehicleName;
        this.vehiclePlateSnapshot = plate;
    }

    public void changeVehicleSize(LavaVehicleSize vehicleSize) {
        requireOpen();
        this.vehicleSize = vehicleSize;
    }

    public void updateOpenTotal(long subtotalCents) {
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
        this.status = LavaWorkOrderStatus.PAYMENT_PENDING;
    }

    public void reopenAfterCheckoutRelease(UUID releasedCheckoutId) {
        if (status != LavaWorkOrderStatus.PAYMENT_PENDING) {
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
        this.status = LavaWorkOrderStatus.OPEN;
    }

    public boolean markPaid(
            UUID paidCheckoutSessionId,
            Long actorUserId,
            OffsetDateTime paidAt
    ) {
        if (status == LavaWorkOrderStatus.PAID
                || status == LavaWorkOrderStatus.COMPLETED) {
            return false;
        }

        if (status != LavaWorkOrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_NOT_PAYMENT_PENDING",
                    "A ordem de servico nao esta aguardando pagamento."
            );
        }

        if (checkoutSessionId == null
                || !checkoutSessionId.equals(paidCheckoutSessionId)) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_CHECKOUT_LINK_INVALID",
                    "O vinculo entre a ordem de servico e o checkout e invalido."
            );
        }

        this.status = LavaWorkOrderStatus.PAID;
        this.paidAt = paidAt;
        this.paidByUserId = actorUserId;

        return true;
    }

    public boolean complete(
            Long actorUserId,
            OffsetDateTime completedAt
    ) {
        if (status == LavaWorkOrderStatus.COMPLETED) {
            return false;
        }

        if (status != LavaWorkOrderStatus.PAID) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_NOT_PAID",
                    "Somente uma ordem de servico paga pode ser concluida."
            );
        }

        this.status = LavaWorkOrderStatus.COMPLETED;
        this.completedAt = completedAt;
        this.completedByUserId = actorUserId;

        return true;
    }

    public void cancelOpen(
            Long actorUserId,
            String reason,
            OffsetDateTime cancelledAt
    ) {
        if (status == LavaWorkOrderStatus.CANCELLED) {
            return;
        }

        requireOpen();

        this.status = LavaWorkOrderStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.cancelledByUserId = actorUserId;
        this.cancellationReason = reason;
    }

    public boolean isPrepared() {
        return status == LavaWorkOrderStatus.PAYMENT_PENDING
                && preparedAt != null;
    }

    public void requireOpen() {
        if (status != LavaWorkOrderStatus.OPEN) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_NOT_OPEN",
                    "A ordem de servico nao esta aberta para alteracoes."
            );
        }
    }
}
