package br.com.vitrine7.lava.workorder.entity;

import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "lava_work_order_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LavaWorkOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "service_name_snapshot", nullable = false, length = 120)
    private String serviceNameSnapshot;

    @Column(
            name = "normalized_service_name_snapshot",
            nullable = false,
            length = 120
    )
    private String normalizedServiceNameSnapshot;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static LavaWorkOrderLineEntity create(
            Long workOrderId,
            LavaServiceEntity service,
            LavaVehicleSize vehicleSize
    ) {
        LavaWorkOrderLineEntity line =
                new LavaWorkOrderLineEntity();

        line.workOrderId = workOrderId;
        line.serviceId = service.getId();
        line.refreshSnapshot(service, vehicleSize);

        return line;
    }

    public void refreshSnapshot(
            LavaServiceEntity service,
            LavaVehicleSize vehicleSize
    ) {
        this.serviceNameSnapshot = service.getName();
        this.normalizedServiceNameSnapshot =
                service.getNormalizedName();
        this.priceCents = vehicleSize == LavaVehicleSize.SMALL
                ? service.getSmallVehiclePriceCents()
                : service.getMediumVehiclePriceCents();
    }
}
