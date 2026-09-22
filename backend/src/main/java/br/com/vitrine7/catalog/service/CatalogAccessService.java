package br.com.vitrine7.catalog.service;

import br.com.vitrine7.catalog.exception.CatalogAccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class CatalogAccessService {

    private final String protectedPassword;

    public CatalogAccessService(
            @Value("${CATALOG_PROTECTED_PASSWORD:}") String protectedPassword
    ) {
        this.protectedPassword = protectedPassword;
    }

    public boolean isPasswordValid(String suppliedPassword) {
        if (protectedPassword == null || protectedPassword.isEmpty() || suppliedPassword == null) {
            return false;
        }

        return MessageDigest.isEqual(
                protectedPassword.getBytes(StandardCharsets.UTF_8),
                suppliedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    public void requireAccess(String suppliedPassword) {
        if (!isPasswordValid(suppliedPassword)) {
            throw new CatalogAccessDeniedException();
        }
    }
}
