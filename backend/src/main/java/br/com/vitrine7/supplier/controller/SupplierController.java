package br.com.vitrine7.supplier.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.supplier.dto.CreateSupplierRequest;
import br.com.vitrine7.supplier.dto.SupplierDetailResponse;
import br.com.vitrine7.supplier.dto.SupplierResponse;
import br.com.vitrine7.supplier.dto.UpdateSupplierRequest;
import br.com.vitrine7.supplier.service.SupplierService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('bar:manage-catalog')")
public class SupplierController {
    private final SupplierService service;
    @GetMapping public PageResponse<SupplierResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "ASC") String direction, @RequestParam(required = false) String search) { return service.list(page, size, sort, direction, search); }
    @GetMapping("/{id}") public SupplierDetailResponse findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest request) { return service.create(request); }
    @PutMapping("/{id}") public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSupplierRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal VitrineUserPrincipal principal) { service.delete(id, principal.getId()); return ResponseEntity.noContent().build(); }
}
