package br.com.vitrine7.receipt.service;

import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ReceiptAuthorizationVerifier {

    public void verify(
            String operationType,
            VitrineUserPrincipal principal
    ) {
        String required = switch (operationType) {
            case "BAR_COMMAND" -> "bar:access";
            case "LAVA_WORK_ORDER" -> "lava:access";
            default -> throw new AuthorizationDeniedException(
                    "Access denied"
            );
        };

        boolean allowed = principal.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        required.equals(authority.getAuthority())
                );

        if (!allowed) {
            throw new AuthorizationDeniedException("Access denied");
        }
    }
}
