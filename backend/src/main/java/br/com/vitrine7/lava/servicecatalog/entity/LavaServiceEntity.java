package br.com.vitrine7.lava.servicecatalog.entity;

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
@Table(name = "lava_services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LavaServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 120
    )
    private String name;

    @Column(
            name = "normalized_name",
            nullable = false,
            length = 120
    )
    private String normalizedName;

    @Column(
            nullable = false,
            length = 80
    )
    private String category;

    @Column(
            name = "normalized_category",
            nullable = false,
            length = 80
    )
    private String normalizedCategory;

    @Column(
            name = "small_vehicle_price_cents",
            nullable = false
    )
    private Long smallVehiclePriceCents;

    @Column(
            name = "medium_vehicle_price_cents",
            nullable = false
    )
    private Long mediumVehiclePriceCents;

    @Column(
            name = "duration_minutes",
            nullable = false
    )
    private Integer durationMinutes;

    @Column(nullable = false)
    private boolean active;

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

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;

    @Version
    @Column(nullable = false)
    private Long version;

    public static LavaServiceEntity create(
            String name,
            String normalizedName,
            String category,
            String normalizedCategory,
            Long smallVehiclePriceCents,
            Long mediumVehiclePriceCents,
            Integer durationMinutes
    ) {
        LavaServiceEntity service =
                new LavaServiceEntity();

        service.name = name;
        service.normalizedName = normalizedName;
        service.category = category;
        service.normalizedCategory = normalizedCategory;
        service.smallVehiclePriceCents =
                smallVehiclePriceCents;
        service.mediumVehiclePriceCents =
                mediumVehiclePriceCents;
        service.durationMinutes = durationMinutes;
        service.active = true;

        return service;
    }

    public void update(
            String name,
            String normalizedName,
            String category,
            String normalizedCategory,
            Long smallVehiclePriceCents,
            Long mediumVehiclePriceCents,
            Integer durationMinutes
    ) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.category = category;
        this.normalizedCategory = normalizedCategory;
        this.smallVehiclePriceCents =
                smallVehiclePriceCents;
        this.mediumVehiclePriceCents =
                mediumVehiclePriceCents;
        this.durationMinutes = durationMinutes;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void softDelete(Long actorUserId) {
        this.active = false;
        this.deletedAt = OffsetDateTime.now();
        this.deletedByUserId = actorUserId;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
