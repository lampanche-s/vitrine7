package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.dto.UpsertBarTabLineRequest;
import br.com.vitrine7.bar.tab.dto.AddBarTabLineRequest;
import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.repository.BarTabLineRepository;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.print.repository.PrintJobRepository;
import br.com.vitrine7.print.service.OperationalOrderRenderer;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BarTabAutomaticOrderPrintTest {

    @Mock private BarTabRepository tabRepository;
    @Mock private BarTabLineRepository lineRepository;
    @Mock private CatalogEntryRepository catalogEntryRepository;
    @Mock private PrintJobRepository printJobRepository;
    @Mock private OperationalOrderRenderer operationalOrderRenderer;
    @Mock private Clock clock;
    @InjectMocks private BarTabService service;

    @Test
    void queuesOnlyNewProductQuantityAndNotReductionsOrPriceEdits() throws Exception {
        assertQueuesOnlyAddedQuantity(CatalogEntryType.ITEM, "ITEM_ORDER");
    }

    @Test
    void queuesOnlyNewServiceQuantityAndNotReductionsOrPriceEdits() throws Exception {
        assertQueuesOnlyAddedQuantity(CatalogEntryType.SERVICE, "SERVICE_ORDER");
    }

    private void assertQueuesOnlyAddedQuantity(
            CatalogEntryType entryType,
            String documentKind
    ) throws Exception {
        try (AutoCloseable ignored = MockitoAnnotations.openMocks(this)) {
            BarTabEntity tab = mock(BarTabEntity.class);
            CatalogEntryEntity entry = mock(CatalogEntryEntity.class);
            VitrineUserPrincipal actor = mock(VitrineUserPrincipal.class);
            BarTabLineEntity[] saved = new BarTabLineEntity[1];

            when(tab.getId()).thenReturn(44L);
            when(tab.getName()).thenReturn("Mesa 4");
            when(tab.getClientId()).thenReturn(null);
            when(tab.getStatus()).thenReturn(BarTabStatus.OPEN);
            if (entryType == CatalogEntryType.SERVICE) {
                when(tab.hasCompleteVehicleSnapshot()).thenReturn(true);
            }
            when(tabRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(tab));
            when(catalogEntryRepository.findByIdAndDeletedAtIsNull(5L))
                    .thenReturn(Optional.of(entry));
            when(entry.getId()).thenReturn(5L);
            when(entry.getEntryType()).thenReturn(entryType);
            when(entry.getName()).thenReturn("Coca-Cola");
            when(entry.getPriceCents()).thenReturn(1000L);
            when(actor.getId()).thenReturn(7L);
            when(actor.getName()).thenReturn("Romario");
            when(clock.instant()).thenReturn(Instant.parse("2026-10-03T23:15:00Z"));
            when(clock.getZone()).thenReturn(ZoneOffset.UTC);
            when(lineRepository.findByTabIdAndCatalogEntryId(44L, 5L))
                    .thenAnswer(call -> Optional.ofNullable(saved[0]));
            when(lineRepository.saveAndFlush(any(BarTabLineEntity.class)))
                    .thenAnswer(call -> saved[0] = call.getArgument(0));
            when(lineRepository.findAllByTabIdOrderByIdAsc(44L))
                    .thenAnswer(call -> saved[0] == null ? List.of() : List.of(saved[0]));

            service.upsertCatalogEntry(44L, 5L, new UpsertBarTabLineRequest(1, null), actor);
            service.upsertCatalogEntry(44L, 5L, new UpsertBarTabLineRequest(3, null), actor);
            service.upsertCatalogEntry(44L, 5L, new UpsertBarTabLineRequest(2, null), actor);
            service.upsertCatalogEntry(44L, 5L, new UpsertBarTabLineRequest(2, 1200L), actor);

            verify(operationalOrderRenderer).renderAddedEntry(
                    eq("Mesa 4"), eq(entryType), eq("Coca-Cola"), eq(1), eq(1000L),
                    eq("Romario"), any(OffsetDateTime.class));
            verify(operationalOrderRenderer).renderAddedEntry(
                    eq("Mesa 4"), eq(entryType), eq("Coca-Cola"), eq(2), eq(1000L),
                    eq("Romario"), any(OffsetDateTime.class));
            verify(printJobRepository, times(2)).createOperationalOrder(
                    any(UUID.class), eq(44L), eq(7L), eq(documentKind), any());

            for (int index = 0; index < 9; index++) {
                service.addCatalogEntry(
                        44L, 5L, new AddBarTabLineRequest(1, null, null), actor
                );
            }

            assertEquals(11, saved[0].getQuantity());
            verify(printJobRepository, times(11)).createOperationalOrder(
                    any(UUID.class), eq(44L), eq(7L), eq(documentKind), any());
        }
    }
}
