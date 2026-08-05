package br.com.vitrine7.payment.terminal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_terminal_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTerminalSettingsEntity {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentTerminalProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTerminalMode mode;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulated_outcome", nullable = false, length = 20)
    private TerminalSimulationOutcome simulatedOutcome;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public void updateSimulatedConfiguration(
            PaymentTerminalProvider provider,
            boolean active,
            TerminalSimulationOutcome simulatedOutcome
    ) {
        this.provider = provider;
        this.mode = PaymentTerminalMode.SIMULATED;
        this.active = active;
        this.simulatedOutcome = simulatedOutcome;
    }
}
