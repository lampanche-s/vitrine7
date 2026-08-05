package br.com.vitrine7.lava.client.specification;

import br.com.vitrine7.lava.client.entity.LavaClientEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LavaClientSpecifications {

    private LavaClientSpecifications() {
    }

    public static Specification<LavaClientEntity> notDeleted() {
        return (root, query, builder) ->
                builder.isNull(root.get("deletedAt"));
    }

    public static Specification<LavaClientEntity> matchesSearch(
            String search
    ) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }

        String normalizedText =
                normalizeText(search);

        String compactSearch = normalizedText
                .replaceAll("[^a-z0-9]", "");

        String phoneDigits = search
                .replaceAll("\\D", "");

        return (root, query, builder) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (!normalizedText.isBlank()) {
                String textPattern =
                        "%" + normalizedText + "%";

                predicates.add(
                        builder.like(
                                root.get("normalizedName"),
                                textPattern
                        )
                );

                predicates.add(
                        builder.like(
                                root.get(
                                        "normalizedVehicleName"
                                ),
                                textPattern
                        )
                );
            }

            if (!compactSearch.isBlank()) {
                predicates.add(
                        builder.like(
                                builder.lower(
                                        root.get("plate")
                                ),
                                "%" + compactSearch + "%"
                        )
                );
            }

            if (!phoneDigits.isBlank()) {
                predicates.add(
                        builder.like(
                                root.get("phoneDigits"),
                                "%" + phoneDigits + "%"
                        )
                );
            }

            return builder.or(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    public static Specification<LavaClientEntity> hasActive(
            Boolean active
    ) {
        if (active == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.equal(
                        root.get("active"),
                        active
                );
    }

    private static String normalizeText(String value) {
        String withoutAccents = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
