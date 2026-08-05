package br.com.vitrine7.auth.dto;

import br.com.vitrine7.system.user.security.VitrineUserPrincipal;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String name,
        String username,
        String role,
        String status,
        List<String> permissions
) {

    public static CurrentUserResponse from(
            VitrineUserPrincipal principal
    ) {
        return new CurrentUserResponse(
                principal.getId(),
                principal.getName(),
                principal.getUsername(),
                principal.getRole().name(),
                principal.getStatus().name(),
                principal.getPermissionCodes()
        );
    }
}
