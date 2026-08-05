package br.com.vitrine7.bar.tab.entity;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
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
@Table(name = "bar_tab_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarTabLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "tab_id",
            nullable = false
    )
    private Long tabId;

    @Column(
            name = "catalog_entry_id",
            nullable = false
    )
    private Long catalogEntryId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "entry_type_snapshot",
            nullable = false,
            length = 20
    )
    private CatalogEntryType entryTypeSnapshot;

    @Column(
            name = "item_name_snapshot",
            nullable = false,
            length = 120
    )
    private String itemNameSnapshot;

    @Column(
            name = "unit_price_cents",
            nullable = false
    )
    private Long unitPriceCents;

    @Column(nullable = false)
    private Integer quantity;

    @Column(
            name = "line_total_cents",
            nullable = false
    )
    private Long lineTotalCents;

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

    @Version
    @Column(nullable = false)
    private Long version;

    public static BarTabLineEntity catalogEntry(
            Long tabId,
            CatalogEntryEntity entry,
            int quantity,
            Long requestedUnitPriceCents
    ) {
        BarTabLineEntity line =
                new BarTabLineEntity();

        line.tabId = tabId;
        line.catalogEntryId = entry.getId();

        line.refreshCatalogEntry(
                entry,
                quantity,
                requestedUnitPriceCents
        );

        return line;
    }

    public void refreshCatalogEntry(
            CatalogEntryEntity entry,
            int quantity,
            Long requestedUnitPriceCents
    ) {
        this.catalogEntryId =
                entry.getId();

        this.entryTypeSnapshot =
                entry.getEntryType();

        this.itemNameSnapshot =
                entry.getName();

        applyPriceAndQuantity(
                resolveUnitPriceCents(
                        entry.getPriceCents(),
                        requestedUnitPriceCents
                ),
                quantity
        );
    }

    public void recalculate() {
        applyPriceAndQuantity(
                unitPriceCents,
                quantity
        );
    }

    private long resolveUnitPriceCents(
            long catalogUnitPriceCents,
            Long requestedUnitPriceCents
    ) {
        if (requestedUnitPriceCents != null) {
            return requestedUnitPriceCents;
        }

        if (unitPriceCents != null) {
            return unitPriceCents;
        }

        return catalogUnitPriceCents;
    }

    private void applyPriceAndQuantity(
            long unitPriceCents,
            int quantity
    ) {
        this.unitPriceCents =
                unitPriceCents;

        this.quantity =
                quantity;

        this.lineTotalCents =
                Math.multiplyExact(
                        unitPriceCents,
                        quantity
                );
    }
}
