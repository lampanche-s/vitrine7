package br.com.vitrine7.system.user.specification;

import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> notDeleted() {
        return (root, query, builder) ->
                builder.isNull(root.get("deletedAt"));
    }

    public static Specification<UserEntity> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%"
                + search.trim().toLowerCase(Locale.ROOT)
                + "%";

        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("username")), pattern)
        );
    }

    public static Specification<UserEntity> hasStatus(UserStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.equal(root.get("status"), status);
    }

    public static Specification<UserEntity> hasRole(UserRole role) {
        if (role == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.equal(root.get("role"), role);
    }

    public static Specification<UserEntity> notRole(UserRole role) {
        return (root, query, builder) ->
                builder.notEqual(root.get("role"), role);
    }

    public static Specification<UserEntity> notId(Long id) {
        if (id == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) ->
                builder.notEqual(root.get("id"), id);
    }
}
