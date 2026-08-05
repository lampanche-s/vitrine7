package br.com.vitrine7.lava.client.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.lava.client.dto.ChangeLavaClientStatusRequest;
import br.com.vitrine7.lava.client.dto.CreateLavaClientRequest;
import br.com.vitrine7.lava.client.dto.LavaClientResponse;
import br.com.vitrine7.lava.client.dto.UpdateLavaClientRequest;
import br.com.vitrine7.lava.client.service.LavaClientService;
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
@RequestMapping("/api/v1/lava/clients")
@RequiredArgsConstructor
@Validated
public class LavaClientController {

    private final LavaClientService clientService;

    @GetMapping
    @PreAuthorize("hasAuthority('lava:access')")
    public PageResponse<LavaClientResponse> list(
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
            Boolean active
    ) {
        return clientService.list(
                page,
                size,
                sort,
                direction,
                search,
                active
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('lava:access')")
    public LavaClientResponse findById(
            @PathVariable Long id
    ) {
        return clientService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('lava:access')")
    public LavaClientResponse create(
            @Valid
            @RequestBody CreateLavaClientRequest request
    ) {
        return clientService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('lava:manage-clients')"
    )
    public LavaClientResponse update(
            @PathVariable Long id,
            @Valid
            @RequestBody UpdateLavaClientRequest request
    ) {
        return clientService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize(
            "hasAuthority('lava:manage-clients')"
    )
    public LavaClientResponse changeActive(
            @PathVariable Long id,
            @Valid
            @RequestBody ChangeLavaClientStatusRequest request
    ) {
        return clientService.changeActive(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('lava:manage-clients')"
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        clientService.delete(
                id,
                principal.getId()
        );

        return ResponseEntity.noContent().build();
    }
}
