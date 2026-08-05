package br.com.vitrine7.catalog.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "catalog_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "entry_type",
            nullable = false,
            length = 20
    )
    private CatalogEntryType entryType;

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
            name = "price_cents",
            nullable = false
    )
    private Long priceCents;

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

    public static CatalogEntryEntity create(
            CatalogEntryType entryType,
            String name,
            String normalizedName,
            Long priceCents
    ) {
        CatalogEntryEntity entry =
                new CatalogEntryEntity();

        entry.entryType = entryType;
        entry.name = name;
        entry.normalizedName = normalizedName;
        entry.priceCents = priceCents;

        return entry;
    }

    public void update(
            CatalogEntryType entryType,
            String name,
            String normalizedName,
            Long priceCents
    ) {
        this.entryType = entryType;
        this.name = name;
        this.normalizedName = normalizedName;
        this.priceCents = priceCents;
    }

    public void softDelete(Long actorUserId) {
        this.deletedAt = OffsetDateTime.now();
        this.deletedByUserId = actorUserId;
    }
}
