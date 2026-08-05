package br.com.vitrine7.lava.client.entity;

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
@Table(name = "lava_clients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LavaClientEntity {

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
            name = "phone_digits",
            length = 11
    )
    private String phoneDigits;

    @Column(
            name = "vehicle_name",
            nullable = false,
            length = 120
    )
    private String vehicleName;

    @Column(
            name = "normalized_vehicle_name",
            nullable = false,
            length = 120
    )
    private String normalizedVehicleName;

    @Column(
            nullable = false,
            length = 7
    )
    private String plate;

    @Column(
            name = "visits_count",
            nullable = false
    )
    private Integer visitsCount;

    @Column(
            name = "last_service_label",
            length = 120
    )
    private String lastServiceLabel;

    @Column(name = "last_service_at")
    private OffsetDateTime lastServiceAt;

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

    public static LavaClientEntity create(
            String name,
            String normalizedName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate
    ) {
        LavaClientEntity client =
                new LavaClientEntity();

        client.name = name;
        client.normalizedName = normalizedName;
        client.phoneDigits = phoneDigits;
        client.vehicleName = vehicleName;
        client.normalizedVehicleName =
                normalizedVehicleName;
        client.plate = plate;

        client.visitsCount = 0;
        client.lastServiceLabel = null;
        client.lastServiceAt = null;

        client.active = true;

        return client;
    }

    public void updateRegistrationData(
            String name,
            String normalizedName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate
    ) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.phoneDigits = phoneDigits;
        this.vehicleName = vehicleName;
        this.normalizedVehicleName =
                normalizedVehicleName;
        this.plate = plate;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void registerCompletedService(
            String serviceLabel,
            OffsetDateTime serviceDate
    ) {
        this.visitsCount = visitsCount + 1;
        this.lastServiceLabel = serviceLabel;
        this.lastServiceAt = serviceDate;
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
