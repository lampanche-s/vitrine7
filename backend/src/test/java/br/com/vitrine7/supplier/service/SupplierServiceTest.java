package br.com.vitrine7.supplier.service;

import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.supplier.entity.SupplierEntity;
import br.com.vitrine7.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierServiceTest {

    @Test
    void deletionUnlinksCatalogEntriesBeforeSoftDelete() {
        SupplierRepository supplierRepository = mock(SupplierRepository.class);
        CatalogEntryRepository catalogRepository = mock(CatalogEntryRepository.class);
        SupplierAvailabilityService availabilityService = mock(SupplierAvailabilityService.class);
        SupplierEntity supplier = SupplierEntity.create("Fornecedor", "fornecedor", null, null, null);
        when(availabilityService.lockAvailable(9L)).thenReturn(supplier);

        SupplierService service = new SupplierService(
                supplierRepository,
                catalogRepository,
                new SupplierNormalizer(),
                availabilityService
        );

        service.delete(9L, 3L);

        verify(catalogRepository).clearSupplierBySupplierId(supplier.getId());
        assertNotNull(supplier.getDeletedAt());
    }
}
