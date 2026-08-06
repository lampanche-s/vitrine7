package br.com.vitrine7.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogEntryEntityTest {

    @Test
    void itemTracksAndDecreasesStock() {
        CatalogEntryEntity entry = CatalogEntryEntity.create(
                CatalogEntryType.ITEM,
                "Espeto bovino",
                "espeto bovino",
                1_800L,
                10,
                3
        );

        assertTrue(entry.tracksStock());
        assertTrue(entry.hasAvailableStock(10));

        entry.decreaseStock(4);

        assertEquals(6, entry.getStockQuantity());
        assertTrue(entry.hasAvailableStock(6));
        assertFalse(entry.hasAvailableStock(7));
    }

    @Test
    void serviceDoesNotTrackStock() {
        CatalogEntryEntity entry = CatalogEntryEntity.create(
                CatalogEntryType.SERVICE,
                "Lavagem",
                "lavagem",
                3_000L,
                50,
                5
        );

        assertFalse(entry.tracksStock());
        assertNull(entry.getStockQuantity());
        assertNull(entry.getMinimumStockQuantity());

        entry.decreaseStock(10);

        assertNull(entry.getStockQuantity());
    }

    @Test
    void itemRejectsInvalidStockConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CatalogEntryEntity.create(
                        CatalogEntryType.ITEM,
                        "Espeto",
                        "espeto",
                        1_000L,
                        null,
                        0
                )
        );
    }

    @Test
    void itemRejectsStockDecreaseAboveAvailableQuantity() {
        CatalogEntryEntity entry = CatalogEntryEntity.create(
                CatalogEntryType.ITEM,
                "Espeto",
                "espeto",
                1_000L,
                2,
                1
        );

        assertThrows(
                IllegalStateException.class,
                () -> entry.decreaseStock(3)
        );
        assertEquals(2, entry.getStockQuantity());
    }
}
