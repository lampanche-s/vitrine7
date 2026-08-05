package br.com.vitrine7.system.user.security;

import br.com.vitrine7.system.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionServiceTest {

    private final RolePermissionService service = new RolePermissionService();

    @Test
    void administratorReceivesCurrentProductPermissionsOnly() {
        Set<Permission> permissions = service.getPermissions(
                UserRole.ADMINISTRADOR
        );

        assertTrue(permissions.contains(Permission.BAR_ACCESS));
        assertTrue(permissions.contains(Permission.BAR_MANAGE_CATALOG));
        assertTrue(permissions.contains(Permission.CLIENTS_MANAGE));
        assertTrue(permissions.contains(Permission.REPORTS_ACCESS));
        assertTrue(permissions.contains(Permission.ADMIN_USERS));
        assertTrue(permissions.contains(Permission.ADMIN_PAYMENT_CONFIG));
    }

    @Test
    void operatorAccessesOnlyOperationalBarFlow() {
        assertEquals(
                Set.of(Permission.BAR_ACCESS),
                service.getPermissions(UserRole.OPERADOR)
        );
    }
}
