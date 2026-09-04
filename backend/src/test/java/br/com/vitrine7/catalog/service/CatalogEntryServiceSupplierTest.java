package br.com.vitrine7.catalog.service;

import br.com.vitrine7.catalog.dto.CreateCatalogEntryRequest;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.supplier.service.SupplierAvailabilityService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CatalogEntryServiceSupplierTest {

    private final CatalogEntryService service = new CatalogEntryService(
            mock(CatalogEntryRepository.class),
            new CatalogEntryTextNormalizer(),
            mock(SupplierAvailabilityService.class)
    );

    @Test
    void rejectsExplicitSupplierForService() {
        InvalidRequestException error = assertThrows(
                InvalidRequestException.class,
                () -> service.create(new CreateCatalogEntryRequest(
                        "Serviço",
                        CatalogEntryType.SERVICE,
                        1_000L,
                        false,
                        null,
                        null,
                        10L
                ))
        );

        assertEquals("CATALOG_SERVICE_SUPPLIER_NOT_ALLOWED", error.getCode());
    }
}
