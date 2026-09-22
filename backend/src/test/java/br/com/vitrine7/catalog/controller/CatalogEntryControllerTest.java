package br.com.vitrine7.catalog.controller;

import br.com.vitrine7.catalog.dto.CreateCatalogEntryRequest;
import br.com.vitrine7.catalog.dto.UpdateCatalogEntryRequest;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import br.com.vitrine7.catalog.exception.CatalogAccessDeniedException;
import br.com.vitrine7.catalog.service.CatalogAccessService;
import br.com.vitrine7.catalog.service.CatalogEntryService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogEntryControllerTest {
    private final CatalogEntryService service = mock(CatalogEntryService.class);
    private final CatalogEntryController controller = new CatalogEntryController(service, new CatalogAccessService("segredo"));
    private final UpdateCatalogEntryRequest update = new UpdateCatalogEntryRequest("Item", CatalogEntryType.ITEM, 100L, false, null, null, null);

    @Test
    void blocksUpdateBeforeCallingServiceWhenPasswordIsAbsent() {
        assertThrows(CatalogAccessDeniedException.class, () -> controller.update(1L, null, update));
        verify(service, never()).update(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void permitsUpdateAndDeleteWithValidPassword() {
        controller.update(1L, "segredo", update);
        verify(service).update(1L, update);
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        when(principal.getId()).thenReturn(7L);
        controller.delete(1L, "segredo", principal);
        verify(service).delete(1L, 7L);
    }

    @Test
    void createDoesNotRequireTheCatalogPassword() {
        controller.create(new CreateCatalogEntryRequest("Item", CatalogEntryType.ITEM, 100L, false, null, null, null));
        verify(service).create(org.mockito.ArgumentMatchers.any());
    }
}
