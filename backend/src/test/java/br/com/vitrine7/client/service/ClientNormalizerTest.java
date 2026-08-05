package br.com.vitrine7.client.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientNormalizerTest {

    private final ClientNormalizer normalizer = new ClientNormalizer();

    @Test
    void normalizesCanonicalClientData() {
        ClientNormalizer.NormalizedClientData data = normalizer.normalize(
                "  Ana   Silva ",
                "(71) 99999-0000",
                "  Honda   Civic ",
                "abc-1d23"
        );

        assertEquals("Ana Silva", data.name());
        assertEquals("ana silva", data.normalizedName());
        assertEquals("71999990000", data.phoneDigits());
        assertEquals("Honda Civic", data.vehicleName());
        assertEquals("honda civic", data.normalizedVehicleName());
        assertEquals("ABC1D23", data.plate());
    }

    @Test
    void acceptsOptionalPhoneAndOldPlate() {
        ClientNormalizer.NormalizedClientData data = normalizer.normalize(
                "Joao",
                "",
                "Gol",
                "ABC-1234"
        );

        assertNull(data.phoneDigits());
        assertEquals("ABC1234", data.plate());
    }

    @Test
    void rejectsInvalidPhoneAndPlate() {
        InvalidRequestException phoneError = assertThrows(
                InvalidRequestException.class,
                () -> normalizer.normalize(
                        "Ana",
                        "123",
                        "Civic",
                        "ABC1D23"
                )
        );

        assertEquals("INVALID_CLIENT_PHONE", phoneError.getCode());

        InvalidRequestException plateError = assertThrows(
                InvalidRequestException.class,
                () -> normalizer.normalize(
                        "Ana",
                        null,
                        "Civic",
                        "INVALIDA"
                )
        );

        assertEquals("INVALID_VEHICLE_PLATE", plateError.getCode());
    }
}
