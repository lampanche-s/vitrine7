package br.com.vitrine7.receipt.service;

import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ReceiptAuthorizationVerifier {

    public void verify(String operationType, VitrineUserPrincipal principal) {
        if (!"BAR_COMMAND".equals(operationType)) {
            throw new AuthorizationDeniedException("Access denied");
        }

        boolean allowed = principal.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "bar:access".equals(authority.getAuthority())
                );

        if (!allowed) {
            throw new AuthorizationDeniedException("Access denied");
        }
    }
}
