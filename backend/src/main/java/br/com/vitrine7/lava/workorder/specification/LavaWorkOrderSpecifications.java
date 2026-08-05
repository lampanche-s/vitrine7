package br.com.vitrine7.lava.workorder.specification;

import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import org.springframework.data.jpa.domain.Specification;

public final class LavaWorkOrderSpecifications {

    private LavaWorkOrderSpecifications() {
    }

    public static Specification<LavaWorkOrderEntity> hasStatus(
            LavaWorkOrderStatus status
    ) {
        return (root, query, builder) ->
                status == null
                        ? builder.conjunction()
                        : builder.equal(root.get("status"), status);
    }

    public static Specification<LavaWorkOrderEntity> matchesSearch(
            String normalizedSearch,
            String digitsSearch,
            String plateSearch
    ) {
        return (root, query, builder) -> {
            if ((normalizedSearch == null || normalizedSearch.isBlank())
                    && (digitsSearch == null || digitsSearch.isBlank())
                    && (plateSearch == null || plateSearch.isBlank())) {
                return builder.conjunction();
            }

            String like = "%"
                    + (normalizedSearch == null ? "" : normalizedSearch)
                    + "%";
            String digitsLike = "%"
                    + (digitsSearch == null ? "" : digitsSearch)
                    + "%";
            String plateLike = "%"
                    + (plateSearch == null ? "" : plateSearch)
                    + "%";

            return builder.or(
                    normalizedSearch == null
                            ? builder.disjunction()
                            : builder.like(
                            root.get("normalizedCustomerNameSnapshot"),
                            like
                    ),
                    normalizedSearch == null
                            ? builder.disjunction()
                            : builder.like(
                            root.get("normalizedVehicleNameSnapshot"),
                            like
                    ),
                    digitsSearch == null
                            ? builder.disjunction()
                            : builder.like(
                            root.get("customerPhoneDigitsSnapshot"),
                            digitsLike
                    ),
                    plateSearch == null
                            ? builder.disjunction()
                            : builder.like(
                            root.get("vehiclePlateSnapshot"),
                            plateLike
                    )
            );
        };
    }
}
