package br.com.vitrine7.supplier.entity;

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
@Table(name = "suppliers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    @Column(name = "cnpj_digits", length = 14)
    private String cnpjDigits;

    @Column(name = "phone_digits", length = 11)
    private String phoneDigits;

    @Column(name = "cep_digits", length = 8)
    private String cepDigits;

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

    public static SupplierEntity create(
            String name,
            String normalizedName,
            String cnpjDigits,
            String phoneDigits,
            String cepDigits
    ) {
        SupplierEntity supplier = new SupplierEntity();
        supplier.update(name, normalizedName, cnpjDigits, phoneDigits, cepDigits);
        return supplier;
    }

    public void update(
            String name,
            String normalizedName,
            String cnpjDigits,
            String phoneDigits,
            String cepDigits
    ) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.cnpjDigits = cnpjDigits;
        this.phoneDigits = phoneDigits;
        this.cepDigits = cepDigits;
    }

    public void softDelete(Long actorUserId) {
        this.deletedAt = OffsetDateTime.now();
        this.deletedByUserId = actorUserId;
    }
}
