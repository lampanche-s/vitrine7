package br.com.vitrine7.bar.tab.controller;

import br.com.vitrine7.bar.tab.dto.BarTabCancellationResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.dto.CancelBarTabRequest;
import br.com.vitrine7.bar.tab.dto.CreateBarTabRequest;
import br.com.vitrine7.bar.tab.dto.PrepareBarTabRequest;
import br.com.vitrine7.bar.tab.dto.RenameBarTabRequest;
import br.com.vitrine7.bar.tab.dto.UpsertBarTabLineRequest;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.service.BarTabCancellationService;
import br.com.vitrine7.bar.tab.service.BarTabService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bar/tabs")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('bar:access')")
public class BarTabController {

    private final BarTabService tabService;
    private final BarTabCancellationService cancellationService;

    @PostMapping
    public ResponseEntity<BarTabResponse> create(
            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            CreateBarTabRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return operationResponse(
                tabService.create(
                        idempotencyKey,
                        request,
                        principal
                )
        );
    }

    @GetMapping
    public PageResponse<BarTabResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            BarTabStatus status,

            @RequestParam(required = false)
            String search
    ) {
        return tabService.list(page, size, status, search);
    }

    @GetMapping("/{tabId}")
    public BarTabResponse findById(
            @PathVariable Long tabId
    ) {
        return tabService.findById(tabId);
    }

    @PutMapping("/{tabId}/name")
    public BarTabResponse rename(
            @PathVariable Long tabId,

            @Valid
            @RequestBody
            RenameBarTabRequest request
    ) {
        return tabService.rename(tabId, request);
    }

    @PutMapping(
            "/{tabId}/catalog/{catalogEntryId}"
    )
    public BarTabResponse upsertCatalogEntry(
            @PathVariable Long tabId,
            @PathVariable Long catalogEntryId,

            @Valid
            @RequestBody
            UpsertBarTabLineRequest request
    ) {
        return tabService.upsertCatalogEntry(
                tabId,
                catalogEntryId,
                request
        );
    }

    @DeleteMapping(
            "/{tabId}/catalog/{catalogEntryId}"
    )
    public BarTabResponse removeCatalogEntry(
            @PathVariable Long tabId,
            @PathVariable Long catalogEntryId
    ) {
        return tabService.removeCatalogEntry(
                tabId,
                catalogEntryId
        );
    }

    @PostMapping("/{tabId}/prepare")
    public ResponseEntity<BarTabResponse> prepare(
            @PathVariable Long tabId,

            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            PrepareBarTabRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return operationResponse(
                tabService.prepare(
                        tabId,
                        idempotencyKey,
                        request,
                        principal
                )
        );
    }

    @PostMapping("/{tabId}/cancel")
    public ResponseEntity<BarTabCancellationResponse> cancel(
            @PathVariable Long tabId,

            @Valid
            @RequestBody
            CancelBarTabRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        BarTabCancellationService.CancellationResult result =
                cancellationService.cancel(
                        tabId,
                        request,
                        principal
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(
                        "Idempotent-Replayed",
                        Boolean.toString(result.replayed())
                )
                .body(result.response());
    }

    private ResponseEntity<BarTabResponse> operationResponse(
            BarTabService.OperationResult result
    ) {
        HttpStatus status = result.replayed()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .header(
                        "Idempotent-Replayed",
                        Boolean.toString(result.replayed())
                )
                .body(result.response());
    }
}
