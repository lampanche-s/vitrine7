package br.com.vitrine7.supplier.specification;

import br.com.vitrine7.supplier.entity.SupplierEntity;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.Locale;

public final class SupplierSpecifications {
    private SupplierSpecifications() { }

    public static Specification<SupplierEntity> notDeleted() {
        return (root, query, builder) -> builder.isNull(root.get("deletedAt"));
    }

    public static Specification<SupplierEntity> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String normalized = Normalizer.normalize(search, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return (root, query, builder) -> builder.like(root.get("normalizedName"), "%" + normalized + "%");
    }
}
