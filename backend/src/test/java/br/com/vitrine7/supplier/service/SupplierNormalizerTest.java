package br.com.vitrine7.supplier.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierNormalizerTest {

    private final SupplierNormalizer normalizer = new SupplierNormalizer();

    @Test
    void acceptsNameOnlyAndNormalizesOptionalFields() {
        SupplierNormalizer.NormalizedSupplierData nameOnly = normalizer.normalize("  Café   Brasil ", null, "", "");
        assertEquals("Café Brasil", nameOnly.name());
        assertEquals("cafe brasil", nameOnly.normalizedName());
        assertNull(nameOnly.cnpjDigits());
        assertNull(nameOnly.phoneDigits());
        assertNull(nameOnly.cepDigits());

        SupplierNormalizer.NormalizedSupplierData complete = normalizer.normalize("Fornecedor", "12.345.678/0001-90", "(71) 99999-0000", "40000-000");
        assertEquals("12345678000190", complete.cnpjDigits());
        assertEquals("71999990000", complete.phoneDigits());
        assertEquals("40000000", complete.cepDigits());
    }

    @Test
    void rejectsInvalidOptionalDocuments() {
        InvalidRequestException cnpjError = assertThrows(InvalidRequestException.class, () -> normalizer.normalize("Fornecedor", "123", null, null));
        assertEquals("INVALID_SUPPLIER_CNPJ", cnpjError.getCode());
        InvalidRequestException phoneError = assertThrows(InvalidRequestException.class, () -> normalizer.normalize("Fornecedor", null, "123", null));
        assertEquals("INVALID_SUPPLIER_PHONE", phoneError.getCode());
        InvalidRequestException cepError = assertThrows(InvalidRequestException.class, () -> normalizer.normalize("Fornecedor", null, null, "123"));
        assertEquals("INVALID_SUPPLIER_CEP", cepError.getCode());
    }
}
