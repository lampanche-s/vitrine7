package br.com.vitrine7.catalog.controller;

import br.com.vitrine7.catalog.dto.CatalogAccessRequest;
import br.com.vitrine7.catalog.service.CatalogAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/access")
@PreAuthorize("hasAuthority('bar:manage-catalog')")
public class CatalogAccessController {

    private final CatalogAccessService accessService;

    public CatalogAccessController(CatalogAccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@RequestBody CatalogAccessRequest request) {
        return accessService.isPasswordValid(request.password())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(403).build();
    }
}
