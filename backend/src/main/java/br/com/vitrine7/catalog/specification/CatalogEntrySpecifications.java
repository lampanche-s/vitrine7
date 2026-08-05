package br.com.vitrine7.catalog.specification;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class CatalogEntrySpecifications {

    private CatalogEntrySpecifications() {
    }

    public static Specification<CatalogEntryEntity>
    notDeleted() {
        return (root, query, builder) ->
                builder.isNull(root.get("deletedAt"));
    }

    public static Specification<CatalogEntryEntity>
    matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%"
                + search.trim().toLowerCase(Locale.ROOT)
                + "%";

        return (root, query, builder) ->
                builder.like(
                        builder.lower(
                                root.get("normalizedName")
                        ),
                        pattern
                );
    }

    public static Specification<CatalogEntryEntity>
    hasType(CatalogEntryType type) {
        if (type == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.equal(
                        root.get("entryType"),
                        type
                );
    }
}
