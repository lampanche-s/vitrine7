package br.com.vitrine7.catalog.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CatalogEntryTextNormalizer {

    public NormalizedCatalogEntryName normalize(
            String rawName
    ) {
        if (rawName == null) {
            throw invalidName();
        }

        String name = rawName
                .trim()
                .replaceAll("\\s+", " ");

        if (name.isBlank()) {
            throw invalidName();
        }

        if (name.length() > 120) {
            throw new InvalidRequestException(
                    "CATALOG_ENTRY_NAME_TOO_LONG",
                    "O nome deve possuir no máximo 120 caracteres."
            );
        }

        return new NormalizedCatalogEntryName(
                name,
                name.toLowerCase(Locale.ROOT)
        );
    }

    private InvalidRequestException invalidName() {
        return new InvalidRequestException(
                "INVALID_CATALOG_ENTRY_NAME",
                "Informe um nome válido."
        );
    }

    public record NormalizedCatalogEntryName(
            String name,
            String normalizedName
    ) {
    }
}
