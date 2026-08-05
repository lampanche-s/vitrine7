package br.com.vitrine7.catalog.controller;

import br.com.vitrine7.catalog.dto.CatalogEntryResponse;
import br.com.vitrine7.catalog.dto.CreateCatalogEntryRequest;
import br.com.vitrine7.catalog.dto.UpdateCatalogEntryRequest;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import br.com.vitrine7.catalog.service.CatalogEntryService;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('bar:access')")
public class CatalogEntryController {

    private final CatalogEntryService service;

    @GetMapping
    public PageResponse<CatalogEntryResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(defaultValue = "name")
            String sort,

            @RequestParam(defaultValue = "ASC")
            String direction,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            CatalogEntryType type
    ) {
        return service.list(
                page,
                size,
                sort,
                direction,
                search,
                type
        );
    }

    @GetMapping("/{id}")
    public CatalogEntryResponse findById(
            @PathVariable Long id
    ) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('bar:manage-catalog')")
    public CatalogEntryResponse create(
            @Valid
            @RequestBody CreateCatalogEntryRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('bar:manage-catalog')")
    public CatalogEntryResponse update(
            @PathVariable Long id,
            @Valid
            @RequestBody UpdateCatalogEntryRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('bar:manage-catalog')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        service.delete(
                id,
                principal.getId()
        );

        return ResponseEntity.noContent().build();
    }
}
