package br.com.vitrine7.bar.tab.specification;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import org.springframework.data.jpa.domain.Specification;

public final class BarTabSpecifications {

    private BarTabSpecifications() {
    }

    public static Specification<BarTabEntity> hasStatus(
            BarTabStatus status
    ) {
        return status == null
                ? Specification.unrestricted()
                : (root, query, builder) ->
                builder.equal(root.get("status"), status);
    }

    public static Specification<BarTabEntity> matchesSearch(
            String normalizedSearch
    ) {
        if (normalizedSearch == null || normalizedSearch.isBlank()) {
            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.like(
                        root.get("normalizedName"),
                        "%" + normalizedSearch + "%"
                );
    }
}
