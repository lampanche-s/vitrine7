package br.com.vitrine7.payment.terminal.entity;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
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
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_terminal_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTerminalTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "checkout_session_id", nullable = false)
    private UUID checkoutSessionId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentTerminalProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTerminalMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTerminalTransactionStatus status;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "provider_transaction_id", length = 120)
    private String providerTransactionId;

    @Column(name = "response_code", length = 40)
    private String responseCode;

    @Column(name = "response_message", length = 255)
    private String responseMessage;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "provider_profile_id")
    private UUID providerProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code_snapshot", nullable = false, length = 30)
    private PaymentProviderCode providerCodeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_environment_snapshot", nullable = false, length = 30)
    private PaymentProviderEnvironment providerEnvironmentSnapshot;

    @Column(name = "provider_configuration_version", nullable = false)
    private Long providerConfigurationVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_status", length = 30)
    private ProviderPaymentStatus providerStatus;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "provider_request_id", length = 120)
    private String providerRequestId;

    @Column(name = "provider_failure_code", length = 80)
    private String providerFailureCode;

    @Column(name = "provider_failure_message", length = 255)
    private String providerFailureMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> providerMetadata = Map.of();

    @Column(name = "last_provider_sync_at")
    private OffsetDateTime lastProviderSyncAt;

    @Column(name = "terminal_device_id")
    private UUID terminalDeviceId;

    @Column(name = "bridge_command_id")
    private UUID bridgeCommandId;

    @Column(name = "bridge_delivery_status", length = 30)
    private String bridgeDeliveryStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static PaymentTerminalTransactionEntity sent(
            UUID paymentId,
            UUID checkoutSessionId,
            UUID idempotencyKey,
            String requestFingerprint,
            PaymentTerminalProvider provider,
            PaymentTerminalMode mode,
            PaymentMethod method,
            long amountCents,
            Long actorUserId,
            OffsetDateTime sentAt,
            UUID providerProfileId,
            PaymentProviderCode providerCodeSnapshot,
            PaymentProviderEnvironment providerEnvironmentSnapshot,
            long providerConfigurationVersion
    ) {
        PaymentTerminalTransactionEntity transaction =
                new PaymentTerminalTransactionEntity();

        transaction.paymentId = paymentId;
        transaction.checkoutSessionId = checkoutSessionId;
        transaction.idempotencyKey = idempotencyKey;
        transaction.requestFingerprint = requestFingerprint;
        transaction.provider = provider;
        transaction.mode = mode;
        transaction.method = method;
        transaction.status = PaymentTerminalTransactionStatus.SENT;
        transaction.amountCents = amountCents;
        transaction.sentAt = sentAt;
        transaction.createdByUserId = actorUserId;
        transaction.providerProfileId = providerProfileId;
        transaction.providerCodeSnapshot = providerCodeSnapshot;
        transaction.providerEnvironmentSnapshot =
                providerEnvironmentSnapshot;
        transaction.providerConfigurationVersion =
                providerConfigurationVersion;
        transaction.providerStatus = ProviderPaymentStatus.PROCESSING;
        transaction.providerMetadata = Map.of();
        transaction.lastProviderSyncAt = sentAt;

        return transaction;
    }

    public void markApproved(
            String providerTransactionId,
            String responseCode,
            String responseMessage,
            OffsetDateTime approvedAt,
            String providerRequestId,
            Map<String, Object> providerMetadata
    ) {
        this.status = PaymentTerminalTransactionStatus.APPROVED;
        this.providerTransactionId = providerTransactionId;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.approvedAt = approvedAt;
        this.declinedAt = null;
        this.failedAt = null;
        this.errorCode = null;
        this.errorMessage = null;
        this.providerStatus = ProviderPaymentStatus.APPROVED;
        this.providerReference = providerTransactionId;
        this.providerRequestId = providerRequestId;
        this.providerFailureCode = null;
        this.providerFailureMessage = null;
        this.providerMetadata = providerMetadata == null
                ? Map.of()
                : providerMetadata;
        this.lastProviderSyncAt = approvedAt;
    }

    public void markDeclined(
            String providerTransactionId,
            String responseCode,
            String responseMessage,
            OffsetDateTime declinedAt,
            String providerRequestId,
            String failureCode,
            String failureMessage,
            Map<String, Object> providerMetadata
    ) {
        this.status = PaymentTerminalTransactionStatus.DECLINED;
        this.providerTransactionId = providerTransactionId;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.approvedAt = null;
        this.declinedAt = declinedAt;
        this.failedAt = null;
        this.errorCode = null;
        this.errorMessage = null;
        this.providerStatus = ProviderPaymentStatus.DECLINED;
        this.providerReference = providerTransactionId;
        this.providerRequestId = providerRequestId;
        this.providerFailureCode = failureCode;
        this.providerFailureMessage = failureMessage;
        this.providerMetadata = providerMetadata == null
                ? Map.of()
                : providerMetadata;
        this.lastProviderSyncAt = declinedAt;
    }

    public void markError(
            String errorCode,
            String errorMessage,
            OffsetDateTime failedAt
    ) {
        this.status = PaymentTerminalTransactionStatus.ERROR;
        this.approvedAt = null;
        this.declinedAt = null;
        this.failedAt = failedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.providerStatus = ProviderPaymentStatus.ERROR;
        this.providerFailureCode = errorCode;
        this.providerFailureMessage = errorMessage;
        this.lastProviderSyncAt = failedAt;
    }
}
