package br.com.vitrine7.system.user.security;

import br.com.vitrine7.system.user.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class RolePermissionService {

    public Set<Permission> getPermissions(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> EnumSet.allOf(Permission.class);

            case ADMINISTRADOR -> EnumSet.of(
                    Permission.LAVA_ACCESS,
                    Permission.LAVA_MANAGE_CLIENTS,
                    Permission.LAVA_MANAGE_SERVICES,
                    Permission.BAR_ACCESS,
                    Permission.BAR_MANAGE_CATALOG,
                    Permission.BAR_STOCK_MANAGE,
                    Permission.PAYMENT_REVERSE,
                    Permission.ADMIN_USERS,
                    Permission.ADMIN_SETTINGS,
                    Permission.REPORTS_ACCESS
            );

            case OPERADOR -> EnumSet.of(
                    Permission.LAVA_ACCESS,
                    Permission.BAR_ACCESS
            );
        };
    }
}
