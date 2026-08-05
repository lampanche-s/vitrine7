package br.com.vitrine7.lava.servicecatalog.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.lava.servicecatalog.dto.ChangeLavaServiceStatusRequest;
import br.com.vitrine7.lava.servicecatalog.dto.CreateLavaServiceRequest;
import br.com.vitrine7.lava.servicecatalog.dto.LavaServiceResponse;
import br.com.vitrine7.lava.servicecatalog.dto.UpdateLavaServiceRequest;
import br.com.vitrine7.lava.servicecatalog.service.LavaServiceService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lava/services")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('lava:access')")
public class LavaServiceController {

    private final LavaServiceService serviceService;

    @GetMapping
    public PageResponse<LavaServiceResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "A página não pode ser negativa."
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "O tamanho mínimo é 1."
            )
            @Max(
                    value = 100,
                    message = "O tamanho máximo é 100."
            )
            int size,

            @RequestParam(defaultValue = "name")
            String sort,

            @RequestParam(defaultValue = "ASC")
            String direction,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            Boolean active
    ) {
        return serviceService.list(
                page,
                size,
                sort,
                direction,
                search,
                category,
                active
        );
    }

    @GetMapping("/{id}")
    public LavaServiceResponse findById(
            @PathVariable Long id
    ) {
        return serviceService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "hasAuthority('lava:manage-services')"
    )
    public LavaServiceResponse create(
            @Valid
            @RequestBody CreateLavaServiceRequest request
    ) {
        return serviceService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('lava:manage-services')"
    )
    public LavaServiceResponse update(
            @PathVariable Long id,
            @Valid
            @RequestBody UpdateLavaServiceRequest request
    ) {
        return serviceService.update(
                id,
                request
        );
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize(
            "hasAuthority('lava:manage-services')"
    )
    public LavaServiceResponse changeActive(
            @PathVariable Long id,
            @Valid
            @RequestBody ChangeLavaServiceStatusRequest request
    ) {
        return serviceService.changeActive(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('lava:manage-services')"
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        serviceService.delete(
                id,
                principal.getId()
        );

        return ResponseEntity.noContent().build();
    }
}
