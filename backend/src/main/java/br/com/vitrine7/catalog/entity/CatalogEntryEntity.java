package br.com.vitrine7.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import br.com.vitrine7.supplier.entity.SupplierEntity;

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

    @Column(
            name = "stock_enabled",
            nullable = false
    )
    private boolean stockEnabled;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "minimum_stock_quantity")
    private Integer minimumStockQuantity;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplier;

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
            Long priceCents,
            boolean stockEnabled,
            Integer stockQuantity,
            Integer minimumStockQuantity
    ) {
        return create(
                entryType,
                name,
                normalizedName,
                priceCents,
                stockEnabled,
                stockQuantity,
                minimumStockQuantity,
                null
        );
    }

    public static CatalogEntryEntity create(
            CatalogEntryType entryType,
            String name,
            String normalizedName,
            Long priceCents,
            boolean stockEnabled,
            Integer stockQuantity,
            Integer minimumStockQuantity,
            SupplierEntity supplier
    ) {
        CatalogEntryEntity entry =
                new CatalogEntryEntity();

        entry.entryType = entryType;
        entry.name = name;
        entry.normalizedName = normalizedName;
        entry.priceCents = priceCents;
        entry.applyStockConfiguration(
                entryType,
                stockEnabled,
                stockQuantity,
                minimumStockQuantity
        );
        entry.applySupplier(entryType, supplier);

        return entry;
    }

    public void update(
            CatalogEntryType entryType,
            String name,
            String normalizedName,
            Long priceCents,
            boolean stockEnabled,
            Integer stockQuantity,
            Integer minimumStockQuantity,
            SupplierEntity supplier
    ) {
        this.entryType = entryType;
        this.name = name;
        this.normalizedName = normalizedName;
        this.priceCents = priceCents;
        applyStockConfiguration(
                entryType,
                stockEnabled,
                stockQuantity,
                minimumStockQuantity
        );
        applySupplier(entryType, supplier);
    }

    public boolean tracksStock() {
        return entryType == CatalogEntryType.ITEM
                && stockEnabled;
    }

    public boolean hasAvailableStock(int requestedQuantity) {
        return !tracksStock()
                || (
                        stockQuantity != null
                        && requestedQuantity <= stockQuantity
                );
    }

    public void decreaseStock(int quantity) {
        if (!tracksStock()) {
            return;
        }

        if (quantity <= 0
                || stockQuantity == null
                || quantity > stockQuantity) {
            throw new IllegalStateException(
                    "Estoque insuficiente para concluir a operação."
            );
        }

        stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (!tracksStock()) {
            return;
        }

        if (quantity <= 0 || stockQuantity == null) {
            throw new IllegalStateException(
                    "Quantidade inválida para devolver ao estoque."
            );
        }

        stockQuantity = Math.addExact(stockQuantity, quantity);
    }

    public void softDelete(Long actorUserId) {
        this.deletedAt = OffsetDateTime.now();
        this.deletedByUserId = actorUserId;
    }

    public void clearSupplier() {
        this.supplier = null;
    }

    private void applySupplier(CatalogEntryType type, SupplierEntity supplier) {
        this.supplier = type == CatalogEntryType.SERVICE ? null : supplier;
    }

    private void applyStockConfiguration(
            CatalogEntryType type,
            boolean stockEnabled,
            Integer stockQuantity,
            Integer minimumStockQuantity
    ) {
        if (type == CatalogEntryType.SERVICE
                || !stockEnabled) {
            this.stockEnabled = false;
            this.stockQuantity = null;
            this.minimumStockQuantity = null;
            return;
        }

        if (stockQuantity == null
                || minimumStockQuantity == null
                || stockQuantity < 0
                || minimumStockQuantity < 0) {
            throw new IllegalArgumentException(
                    "Itens precisam de estoque atual e estoque mínimo válidos."
            );
        }

        this.stockEnabled = true;
        this.stockQuantity = stockQuantity;
        this.minimumStockQuantity = minimumStockQuantity;
    }
}
