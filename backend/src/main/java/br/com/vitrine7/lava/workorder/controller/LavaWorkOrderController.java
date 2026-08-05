package br.com.vitrine7.lava.workorder.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.lava.workorder.dto.CancelLavaWorkOrderRequest;
import br.com.vitrine7.lava.workorder.dto.CreateLavaWorkOrderRequest;
import br.com.vitrine7.lava.workorder.dto.LavaWorkOrderResponse;
import br.com.vitrine7.lava.workorder.dto.PrepareLavaWorkOrderRequest;
import br.com.vitrine7.lava.workorder.dto.UpdateLavaWorkOrderCustomerRequest;
import br.com.vitrine7.lava.workorder.dto.UpdateLavaWorkOrderVehicleSizeRequest;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import br.com.vitrine7.lava.workorder.service.LavaWorkOrderCompletionService;
import br.com.vitrine7.lava.workorder.service.LavaWorkOrderService;
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
@RequestMapping("/api/v1/lava/work-orders")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('lava:access')")
public class LavaWorkOrderController {

    private final LavaWorkOrderService workOrderService;
    private final LavaWorkOrderCompletionService completionService;

    @PostMapping
    public ResponseEntity<LavaWorkOrderResponse> create(
            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            CreateLavaWorkOrderRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return operationResponse(
                workOrderService.create(
                        idempotencyKey,
                        request,
                        principal
                ),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public PageResponse<LavaWorkOrderResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            LavaWorkOrderStatus status,

            @RequestParam(required = false)
            String search
    ) {
        return workOrderService.list(
                page,
                size,
                status,
                search
        );
    }

    @GetMapping("/{workOrderId}")
    public LavaWorkOrderResponse findById(
            @PathVariable Long workOrderId
    ) {
        return workOrderService.findById(workOrderId);
    }

    @PutMapping("/{workOrderId}/customer")
    public LavaWorkOrderResponse updateCustomer(
            @PathVariable Long workOrderId,

            @Valid
            @RequestBody
            UpdateLavaWorkOrderCustomerRequest request
    ) {
        return workOrderService.updateCustomer(
                workOrderId,
                request
        );
    }

    @PutMapping("/{workOrderId}/vehicle-size")
    public LavaWorkOrderResponse changeVehicleSize(
            @PathVariable Long workOrderId,

            @Valid
            @RequestBody
            UpdateLavaWorkOrderVehicleSizeRequest request
    ) {
        return workOrderService.changeVehicleSize(
                workOrderId,
                request
        );
    }

    @PutMapping("/{workOrderId}/services/{serviceId}")
    public LavaWorkOrderResponse upsertService(
            @PathVariable Long workOrderId,
            @PathVariable Long serviceId
    ) {
        return workOrderService.upsertService(
                workOrderId,
                serviceId
        );
    }

    @DeleteMapping("/{workOrderId}/services/{serviceId}")
    public LavaWorkOrderResponse removeService(
            @PathVariable Long workOrderId,
            @PathVariable Long serviceId
    ) {
        return workOrderService.removeService(
                workOrderId,
                serviceId
        );
    }

    @PostMapping("/{workOrderId}/prepare")
    public ResponseEntity<LavaWorkOrderResponse> prepare(
            @PathVariable Long workOrderId,

            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            PrepareLavaWorkOrderRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return operationResponse(
                workOrderService.prepare(
                        workOrderId,
                        idempotencyKey,
                        request,
                        principal
                ),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{workOrderId}/cancel")
    public ResponseEntity<LavaWorkOrderResponse> cancel(
            @PathVariable Long workOrderId,

            @Valid
            @RequestBody
            CancelLavaWorkOrderRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return operationResponse(
                workOrderService.cancel(
                        workOrderId,
                        request,
                        principal
                ),
                HttpStatus.OK
        );
    }

    @PostMapping("/{workOrderId}/complete")
    public ResponseEntity<LavaWorkOrderResponse> complete(
            @PathVariable Long workOrderId,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        LavaWorkOrderCompletionService.CompletionResult result =
                completionService.complete(
                        workOrderId,
                        principal
                );

        return ResponseEntity
                .ok()
                .header(
                        "Idempotent-Replayed",
                        Boolean.toString(result.replayed())
                )
                .body(result.response());
    }

    private ResponseEntity<LavaWorkOrderResponse> operationResponse(
            LavaWorkOrderService.OperationResult result,
            HttpStatus createdStatus
    ) {
        HttpStatus status = result.replayed()
                ? HttpStatus.OK
                : createdStatus;

        return ResponseEntity
                .status(status)
                .header(
                        "Idempotent-Replayed",
                        Boolean.toString(result.replayed())
                )
                .body(result.response());
    }
}
