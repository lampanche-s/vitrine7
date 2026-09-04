package br.com.vitrine7.employee.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.employee.dto.*;
import br.com.vitrine7.employee.service.EmployeeService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Validated
public class EmployeeController {
    private final EmployeeService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('bar:access', 'clients:manage')")
    public PageResponse<EmployeeResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('clients:manage')")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('clients:manage')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('clients:manage')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal VitrineUserPrincipal principal) {
        service.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
