package br.com.vitrine7.lava.servicecatalog.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class LavaServiceNormalizer {

    public NormalizedLavaServiceData normalize(
            String rawName,
            String rawCategory
    ) {
        String name = normalizeRequiredText(
                rawName,
                "INVALID_LAVA_SERVICE_NAME",
                "Informe um nome válido para o serviço."
        );

        String category = normalizeRequiredText(
                rawCategory,
                "INVALID_LAVA_SERVICE_CATEGORY",
                "Informe uma categoria válida para o serviço."
        );

        if (name.length() > 120) {
            throw new InvalidRequestException(
                    "LAVA_SERVICE_NAME_TOO_LONG",
                    "O nome deve possuir no máximo 120 caracteres."
            );
        }

        if (category.length() > 80) {
            throw new InvalidRequestException(
                    "LAVA_SERVICE_CATEGORY_TOO_LONG",
                    "A categoria deve possuir no máximo 80 caracteres."
            );
        }

        return new NormalizedLavaServiceData(
                name,
                normalizeForSearch(name),
                category,
                normalizeForSearch(category)
        );
    }

    public String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return normalizeForSearch(value);
    }

    private String normalizeRequiredText(
            String value,
            String code,
            String message
    ) {
        if (value == null) {
            throw new InvalidRequestException(
                    code,
                    message
            );
        }

        String normalized = value
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.isBlank()) {
            throw new InvalidRequestException(
                    code,
                    message
            );
        }

        return normalized;
    }

    private String normalizeForSearch(String value) {
        String withoutAccents = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public record NormalizedLavaServiceData(
            String name,
            String normalizedName,
            String category,
            String normalizedCategory
    ) {
    }
}
