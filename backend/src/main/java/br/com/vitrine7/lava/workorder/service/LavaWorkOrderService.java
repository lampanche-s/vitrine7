package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.checkout.config.CheckoutProperties;
import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.lava.client.entity.LavaClientEntity;
import br.com.vitrine7.lava.client.repository.LavaClientRepository;
import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;
import br.com.vitrine7.lava.servicecatalog.repository.LavaServiceRepository;
import br.com.vitrine7.lava.workorder.dto.CancelLavaWorkOrderRequest;
import br.com.vitrine7.lava.workorder.dto.CreateLavaWorkOrderRequest;
import br.com.vitrine7.lava.workorder.dto.LavaWorkOrderLineResponse;
import br.com.vitrine7.lava.workorder.dto.LavaWorkOrderResponse;
import br.com.vitrine7.lava.workorder.dto.PrepareLavaWorkOrderRequest;
import br.com.vitrine7.lava.workorder.dto.UpdateLavaWorkOrderCustomerRequest;
import br.com.vitrine7.lava.workorder.dto.UpdateLavaWorkOrderVehicleSizeRequest;
import br.com.vitrine7.lava.workorder.entity.LavaVehicleSize;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderLineEntity;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderLineRepository;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderRepository;
import br.com.vitrine7.lava.workorder.specification.LavaWorkOrderSpecifications;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LavaWorkOrderService {

    private static final String CREATE_VERSION =
            "lava-work-order-create-v2";

    private static final String PREPARE_VERSION =
            "lava-work-order-prepare-v1";

    private final LavaWorkOrderRepository workOrderRepository;
    private final LavaWorkOrderLineRepository lineRepository;
    private final LavaClientRepository clientRepository;
    private final LavaServiceRepository serviceRepository;
    private final CheckoutSessionRepository checkoutRepository;
    private final LavaWorkOrderCreationService creationService;
    private final LavaWorkOrderNormalizer normalizer;
    private final IdempotencyFingerprintService fingerprintService;
    private final CheckoutProperties checkoutProperties;
    private final Clock clock;

    public OperationResult create(
            UUID idempotencyKey,
            CreateLavaWorkOrderRequest request,
            VitrineUserPrincipal principal
    ) {
        LavaWorkOrderNormalizer.NormalizedCustomer customer =
                normalizeCustomer(
                        request.clientId(),
                        request.customerName(),
                        request.phone(),
                        request.vehicleName(),
                        request.plate()
                );

        String fingerprint = createFingerprint(
                customer,
                request.vehicleSize(),
                request.serviceId()
        );

        LavaWorkOrderEntity existing = workOrderRepository
                .findByCreateIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existing != null) {
            validateCreateReplay(
                    existing,
                    fingerprint,
                    principal.getId()
            );

            return new OperationResult(
                    buildResponse(existing),
                    true
            );
        }

        try {
            LavaWorkOrderEntity created =
                    creationService.create(
                            customer,
                            request.vehicleSize(),
                            request.serviceId(),
                            idempotencyKey,
                            fingerprint,
                            principal.getId()
                    );


            return new OperationResult(
                    buildResponse(created),
                    false
            );
        } catch (DataIntegrityViolationException exception) {
            LavaWorkOrderEntity concurrent = workOrderRepository
                    .findByCreateIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);

            validateCreateReplay(
                    concurrent,
                    fingerprint,
                    principal.getId()
            );

            return new OperationResult(
                    buildResponse(concurrent),
                    true
            );
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<LavaWorkOrderResponse> list(
            int page,
            int size,
            LavaWorkOrderStatus status,
            String search
    ) {
        String normalizedSearch =
                normalizer.normalizeSearchText(search);
        String digitsSearch =
                normalizer.digitsOnlyOrNull(search);
        String plateSearch =
                normalizer.normalizePlateSearch(search);

        Specification<LavaWorkOrderEntity> specification =
                Specification.allOf(
                        LavaWorkOrderSpecifications.hasStatus(status),
                        LavaWorkOrderSpecifications.matchesSearch(
                                normalizedSearch,
                                digitsSearch,
                                plateSearch
                        )
                );

        Page<LavaWorkOrderEntity> result =
                workOrderRepository.findAll(
                        specification,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "updatedAt"
                                )
                        )
                );

        return PageResponse.from(result, this::buildResponse);
    }

    @Transactional(readOnly = true)
    public LavaWorkOrderResponse findById(Long workOrderId) {
        return buildResponse(getWorkOrder(workOrderId));
    }

    @Transactional
    public LavaWorkOrderResponse updateCustomer(
            Long workOrderId,
            UpdateLavaWorkOrderCustomerRequest request
    ) {
        LavaWorkOrderEntity workOrder =
                getEditableWorkOrder(workOrderId);

        LavaWorkOrderNormalizer.NormalizedCustomer customer =
                normalizeCustomer(
                        request.clientId(),
                        request.customerName(),
                        request.phone(),
                        request.vehicleName(),
                        request.plate()
                );

        workOrder.updateCustomerSnapshot(
                customer.clientId(),
                customer.customerName(),
                customer.normalizedCustomerName(),
                customer.phoneDigits(),
                customer.vehicleName(),
                customer.normalizedVehicleName(),
                customer.plate()
        );

        return buildResponse(workOrder);
    }

    @Transactional
    public LavaWorkOrderResponse changeVehicleSize(
            Long workOrderId,
            UpdateLavaWorkOrderVehicleSizeRequest request
    ) {
        LavaWorkOrderEntity workOrder =
                getEditableWorkOrder(workOrderId);

        workOrder.changeVehicleSize(request.vehicleSize());

        List<LavaWorkOrderLineEntity> lines =
                lineRepository
                        .findAllByWorkOrderIdOrderByIdAsc(workOrderId);

        for (LavaWorkOrderLineEntity line : lines) {
            LavaServiceEntity service =
                    loadAvailableServiceForUpdate(
                            line.getServiceId()
                    );

            line.refreshSnapshot(
                    service,
                    request.vehicleSize()
            );
        }

        refreshOpenTotal(workOrder);

        return buildResponse(workOrder);
    }

    @Transactional
    public LavaWorkOrderResponse upsertService(
            Long workOrderId,
            Long serviceId
    ) {
        LavaWorkOrderEntity workOrder =
                getEditableWorkOrder(workOrderId);

        LavaServiceEntity service =
                loadAvailableServiceForUpdate(serviceId);

        LavaWorkOrderLineEntity line = lineRepository
                .findByWorkOrderIdAndServiceId(
                        workOrderId,
                        serviceId
                )
                .orElseGet(() ->
                        LavaWorkOrderLineEntity.create(
                                workOrderId,
                                service,
                                workOrder.getVehicleSize()
                        )
                );

        line.refreshSnapshot(
                service,
                workOrder.getVehicleSize()
        );

        lineRepository.saveAndFlush(line);
        refreshOpenTotal(workOrder);

        return buildResponse(workOrder);
    }

    @Transactional
    public LavaWorkOrderResponse removeService(
            Long workOrderId,
            Long serviceId
    ) {
        LavaWorkOrderEntity workOrder =
                getEditableWorkOrder(workOrderId);

        int removed = lineRepository
                .deleteByWorkOrderIdAndServiceId(
                        workOrderId,
                        serviceId
                );

        if (removed == 0) {
            throw new NotFoundException(
                    "LAVA_WORK_ORDER_SERVICE_NOT_FOUND",
                    "O servico nao esta presente na ordem de servico."
            );
        }

        lineRepository.flush();
        refreshOpenTotal(workOrder);

        return buildResponse(workOrder);
    }

    @Transactional
    public OperationResult prepare(
            Long workOrderId,
            UUID idempotencyKey,
            PrepareLavaWorkOrderRequest request,
            VitrineUserPrincipal principal
    ) {
        LavaWorkOrderNormalizer.NormalizedPreparation preparation =
                normalizer.normalizePreparation(
                        request.documentType(),
                        request.cpf(),
                        request.discountCents()
                );

        String fingerprint = prepareFingerprint(
                workOrderId,
                preparation
        );

        LavaWorkOrderEntity workOrder =
                getWorkOrderForUpdate(workOrderId);

        if (workOrder.isPrepared()) {
            validatePrepareReplay(
                    workOrder,
                    idempotencyKey,
                    fingerprint
            );

            return new OperationResult(
                    buildResponse(workOrder),
                    true
            );
        }

        workOrder.requireOpen();

        checkoutRepository
                .findByIdempotencyKey(idempotencyKey)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "IDEMPOTENCY_KEY_ALREADY_USED",
                            "A chave de idempotencia ja foi utilizada."
                    );
                });

        List<LavaWorkOrderLineEntity> lines =
                lineRepository
                        .findAllByWorkOrderIdOrderByIdAsc(workOrderId);

        if (lines.isEmpty()) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_EMPTY",
                    "Adicione pelo menos um servico a ordem de servico."
            );
        }

        List<Long> serviceIds = lines.stream()
                .map(LavaWorkOrderLineEntity::getServiceId)
                .distinct()
                .sorted()
                .toList();

        List<LavaServiceEntity> lockedServices =
                serviceRepository.findAllByIdsForUpdate(serviceIds);

        if (lockedServices.size() != serviceIds.size()) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_SERVICE_UNAVAILABLE",
                    "Um ou mais servicos da ordem nao estao disponiveis."
            );
        }

        Map<Long, LavaServiceEntity> servicesById =
                lockedServices.stream()
                        .collect(Collectors.toMap(
                                LavaServiceEntity::getId,
                                Function.identity()
                        ));

        long subtotal = 0L;

        try {
            for (LavaWorkOrderLineEntity line : lines) {
                LavaServiceEntity service =
                        servicesById.get(line.getServiceId());

                validateAvailableService(service);

                line.refreshSnapshot(
                        service,
                        workOrder.getVehicleSize()
                );

                subtotal = Math.addExact(
                        subtotal,
                        line.getPriceCents()
                );
            }
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_AMOUNT_OVERFLOW",
                    "O total da ordem de servico excede o limite permitido."
            );
        }

        if (subtotal <= 0) {
            throw new BusinessException(
                    "INVALID_LAVA_WORK_ORDER_SUBTOTAL",
                    "O subtotal da ordem de servico deve ser maior que zero."
            );
        }

        if (preparation.discountCents() >= subtotal) {
            throw new BusinessException(
                    "INVALID_CHECKOUT_DISCOUNT",
                    "O desconto deve ser menor que o subtotal da ordem de servico."
            );
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(
                checkoutProperties.getDraftExpiration()
        );

        CheckoutSessionEntity checkout =
                CheckoutSessionEntity.openDraft(
                        idempotencyKey,
                        fingerprint,
                        CheckoutOperationType.LAVA_WORK_ORDER,
                        principal.getId(),
                        expiresAt
                );

        checkoutRepository.saveAndFlush(checkout);
        checkout.attachSource(workOrder.getId());
        checkout.prepareForPayment(
                subtotal,
                preparation.discountCents(),
                preparation.documentType(),
                preparation.cpfDigits(),
                expiresAt
        );

        workOrder.markPaymentPending(
                checkout.getId(),
                subtotal,
                preparation.discountCents(),
                idempotencyKey,
                fingerprint,
                now
        );

        lineRepository.flush();
        workOrderRepository.flush();
        checkoutRepository.flush();


        return new OperationResult(
                buildResponse(workOrder),
                false
        );
    }

    @Transactional
    public OperationResult cancel(
            Long workOrderId,
            CancelLavaWorkOrderRequest request,
            VitrineUserPrincipal principal
    ) {
        LavaWorkOrderEntity workOrder =
                getWorkOrderForUpdate(workOrderId);

        boolean replayed =
                workOrder.getStatus()
                        == LavaWorkOrderStatus.CANCELLED;

        if (!replayed) {
            String reason =
                    normalizer.normalizeCancelReason(
                            request.reason()
                    );

            workOrder.cancelOpen(
                    principal.getId(),
                    reason,
                    OffsetDateTime.now(clock)
            );

        }

        return new OperationResult(
                buildResponse(workOrder),
                replayed
        );
    }

    private LavaWorkOrderNormalizer.NormalizedCustomer normalizeCustomer(
            Long clientId,
            String customerName,
            String phone,
            String vehicleName,
            String plate
    ) {
        LavaClientEntity client = null;

        if (clientId != null) {
            client = clientRepository
                    .findByIdAndDeletedAtIsNull(clientId)
                    .orElseThrow(() -> new NotFoundException(
                            "LAVA_CLIENT_NOT_FOUND",
                            "Cliente do lava jato nao encontrado."
                    ));
        }

        return normalizer.normalizeCustomer(
                clientId,
                customerName,
                phone,
                vehicleName,
                plate,
                client
        );
    }

    private LavaServiceEntity loadAvailableServiceForUpdate(
            Long serviceId
    ) {
        LavaServiceEntity service = serviceRepository
                .findByIdForUpdate(serviceId)
                .orElseThrow(() -> new NotFoundException(
                        "LAVA_SERVICE_NOT_FOUND",
                        "Servico do lava jato nao encontrado."
                ));

        validateAvailableService(service);
        return service;
    }

    private void validateAvailableService(
            LavaServiceEntity service
    ) {
        if (service == null
                || service.isDeleted()
                || !service.isActive()) {
            throw new BusinessException(
                    "LAVA_SERVICE_INACTIVE",
                    "O servico selecionado esta inativo."
            );
        }
    }

    private LavaWorkOrderEntity getWorkOrder(Long workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new NotFoundException(
                        "LAVA_WORK_ORDER_NOT_FOUND",
                        "Ordem de servico nao encontrada."
                ));
    }

    private LavaWorkOrderEntity getWorkOrderForUpdate(
            Long workOrderId
    ) {
        return workOrderRepository
                .findByIdForUpdate(workOrderId)
                .orElseThrow(() -> new NotFoundException(
                        "LAVA_WORK_ORDER_NOT_FOUND",
                        "Ordem de servico nao encontrada."
                ));
    }

    private LavaWorkOrderEntity getEditableWorkOrder(
            Long workOrderId
    ) {
        LavaWorkOrderEntity workOrder =
                getWorkOrderForUpdate(workOrderId);
        workOrder.requireOpen();
        return workOrder;
    }

    private void refreshOpenTotal(
            LavaWorkOrderEntity workOrder
    ) {
        long subtotal;

        try {
            subtotal = lineRepository
                    .findAllByWorkOrderIdOrderByIdAsc(
                            workOrder.getId()
                    )
                    .stream()
                    .mapToLong(
                            LavaWorkOrderLineEntity::getPriceCents
                    )
                    .reduce(0L, Math::addExact);
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_AMOUNT_OVERFLOW",
                    "O total da ordem de servico excede o limite permitido."
            );
        }

        workOrder.updateOpenTotal(subtotal);
    }

    public LavaWorkOrderResponse buildResponse(
            LavaWorkOrderEntity workOrder
    ) {
        CheckoutSessionEntity checkout =
                workOrder.getCheckoutSessionId() == null
                        ? null
                        : checkoutRepository
                        .findById(workOrder.getCheckoutSessionId())
                        .orElse(null);

        List<LavaWorkOrderLineResponse> services =
                lineRepository
                        .findAllByWorkOrderIdOrderByIdAsc(
                                workOrder.getId()
                        )
                        .stream()
                        .map(LavaWorkOrderLineResponse::from)
                        .toList();

        return new LavaWorkOrderResponse(
                workOrder.getId(),
                workOrder.getRegisteredClientId(),
                workOrder.getCustomerNameSnapshot(),
                workOrder.getCustomerPhoneDigitsSnapshot(),
                workOrder.getVehicleNameSnapshot(),
                workOrder.getVehiclePlateSnapshot(),
                workOrder.getVehicleSize().name(),
                workOrder.getStatus().name(),
                workOrder.getCheckoutSessionId(),
                checkout == null ? null : checkout.getStatus().name(),
                checkout == null ? null : checkout.getSourceId(),
                checkout == null
                        ? null
                        : checkout.getOperationType().name(),
                workOrder.getSubtotalCents(),
                workOrder.getDiscountCents(),
                workOrder.getTotalCents(),
                checkout == null || checkout.getDocumentType() == null
                        ? null
                        : checkout.getDocumentType().name(),
                checkout == null ? null : checkout.getCpfDigits(),
                workOrder.isPrepared(),
                workOrder.getPreparedAt(),
                workOrder.getPaidAt(),
                workOrder.getPaidByUserId(),
                workOrder.getCompletedAt(),
                workOrder.getCompletedByUserId(),
                workOrder.getCancelledAt(),
                workOrder.getCancelledByUserId(),
                workOrder.getCancellationReason(),
                services,
                workOrder.getCreatedByUserId(),
                workOrder.getCreatedAt(),
                workOrder.getUpdatedAt()
        );
    }

    private String createFingerprint(
            LavaWorkOrderNormalizer.NormalizedCustomer customer,
            LavaVehicleSize vehicleSize,
            Long serviceId
    ) {
        return fingerprintService.sha256(
                CREATE_VERSION
                        + "|clientId="
                        + customer.clientId()
                        + "|customerName="
                        + customer.customerName()
                        + "|phone="
                        + customer.phoneDigits()
                        + "|vehicleName="
                        + customer.vehicleName()
                        + "|plate="
                        + customer.plate()
                        + "|vehicleSize="
                        + vehicleSize.name()
                        + "|serviceId="
                        + serviceId
        );
    }

    private String prepareFingerprint(
            Long workOrderId,
            LavaWorkOrderNormalizer.NormalizedPreparation preparation
    ) {
        return fingerprintService.sha256(
                PREPARE_VERSION
                        + "|workOrderId="
                        + workOrderId
                        + "|documentType="
                        + (
                                preparation.documentType() == null
                                        ? "NONE"
                                        : preparation.documentType().name()
                        )
                        + "|cpf="
                        + preparation.cpfDigits()
                        + "|discountCents="
                        + preparation.discountCents()
        );
    }

    private void validateCreateReplay(
            LavaWorkOrderEntity existing,
            String fingerprint,
            Long actorUserId
    ) {
        if (!existing.getCreatedByUserId().equals(actorUserId)) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_IDEMPOTENCY_KEY_ALREADY_USED",
                    "A chave de idempotencia ja foi utilizada."
            );
        }

        if (!existing.getCreateRequestFingerprint()
                .trim()
                .equals(fingerprint)) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                    "A chave foi reutilizada com uma requisicao diferente."
            );
        }
    }

    private void validatePrepareReplay(
            LavaWorkOrderEntity workOrder,
            UUID idempotencyKey,
            String fingerprint
    ) {
        if (!workOrder.getPrepareIdempotencyKey()
                .equals(idempotencyKey)) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_ALREADY_PREPARED",
                    "A ordem de servico ja foi enviada para pagamento."
            );
        }

        if (!workOrder.getPrepareRequestFingerprint()
                .trim()
                .equals(fingerprint)) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_PREPARE_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                    "A chave foi reutilizada com uma preparacao diferente."
            );
        }
    }

    public record OperationResult(
            LavaWorkOrderResponse response,
            boolean replayed
    ) {
    }
}
