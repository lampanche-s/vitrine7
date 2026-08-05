package br.com.vitrine7.lava.servicecatalog.specification;

import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;
import org.springframework.data.jpa.domain.Specification;

public final class LavaServiceSpecifications {

    private LavaServiceSpecifications() {
    }

    public static Specification<LavaServiceEntity> notDeleted() {
        return (root, query, builder) ->
                builder.isNull(root.get("deletedAt"));
    }

    public static Specification<LavaServiceEntity> matchesSearch(
            String normalizedSearch
    ) {
        if (normalizedSearch == null
                || normalizedSearch.isBlank()) {

            return Specification.unrestricted();
        }

        String pattern =
                "%" + normalizedSearch + "%";

        return (root, query, builder) -> builder.or(
                builder.like(
                        root.get("normalizedName"),
                        pattern
                ),
                builder.like(
                        root.get("normalizedCategory"),
                        pattern
                )
        );
    }

    public static Specification<LavaServiceEntity> hasCategory(
            String normalizedCategory
    ) {
        if (normalizedCategory == null
                || normalizedCategory.isBlank()) {

            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.equal(
                        root.get("normalizedCategory"),
                        normalizedCategory
                );
    }

    public static Specification<LavaServiceEntity> hasActive(
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
}
