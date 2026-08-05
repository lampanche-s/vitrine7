package br.com.vitrine7.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "clients")
public class ClientEntity {

    protected ClientEntity() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    @Column(name = "phone_digits", length = 11)
    private String phoneDigits;

    @Column(name = "vehicle_name", nullable = false, length = 120)
    private String vehicleName;

    @Column(name = "normalized_vehicle_name", nullable = false, length = 120)
    private String normalizedVehicleName;

    @Column(nullable = false, length = 7)
    private String plate;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;

    @Version
    @Column(nullable = false)
    private Long version;

    public static ClientEntity create(
            String name,
            String normalizedName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate
    ) {
        ClientEntity client = new ClientEntity();
        client.name = name;
        client.normalizedName = normalizedName;
        client.phoneDigits = phoneDigits;
        client.vehicleName = vehicleName;
        client.normalizedVehicleName = normalizedVehicleName;
        client.plate = plate;
        client.active = true;
        return client;
    }

    public void update(
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
        this.normalizedVehicleName = normalizedVehicleName;
        this.plate = plate;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void softDelete(Long actorUserId) {
        this.active = false;
        this.deletedAt = OffsetDateTime.now();
        this.deletedByUserId = actorUserId;
    }
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getPhoneDigits() {
        return phoneDigits;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public String getNormalizedVehicleName() {
        return normalizedVehicleName;
    }

    public String getPlate() {
        return plate;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public Long getDeletedByUserId() {
        return deletedByUserId;
    }

    public Long getVersion() {
        return version;
    }

}
