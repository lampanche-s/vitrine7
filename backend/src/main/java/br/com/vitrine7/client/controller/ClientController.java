package br.com.vitrine7.client.controller;

import br.com.vitrine7.client.dto.ChangeClientStatusRequest;
import br.com.vitrine7.client.dto.ClientResponse;
import br.com.vitrine7.client.dto.CreateClientRequest;
import br.com.vitrine7.client.dto.UpdateClientRequest;
import br.com.vitrine7.client.service.ClientService;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/v1/clients")
@Validated
@PreAuthorize("hasAuthority('clients:manage')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public PageResponse<ClientResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return clientService.list(page, size, sort, direction, search, active);
    }

    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable Long id) {
        return clientService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }

    @PutMapping("/{id}")
    public ClientResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request
    ) {
        return clientService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public ClientResponse changeActive(
            @PathVariable Long id,
            @Valid @RequestBody ChangeClientStatusRequest request
    ) {
        return clientService.changeActive(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        clientService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
