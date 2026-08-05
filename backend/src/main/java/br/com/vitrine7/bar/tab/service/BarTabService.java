package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.dto.CreateBarTabRequest;
import br.com.vitrine7.bar.tab.dto.PrepareBarTabRequest;
import br.com.vitrine7.bar.tab.dto.RenameBarTabRequest;
import br.com.vitrine7.bar.tab.dto.UpsertBarTabLineRequest;
import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.repository.BarTabLineRepository;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.bar.tab.specification.BarTabSpecifications;
import br.com.vitrine7.checkout.config.CheckoutProperties;
import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.common.pagination.PageResponse;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BarTabService {

    private static final String CREATE_VERSION =
            "bar-tab-create-v1";

    private static final String PREPARE_VERSION =
            "bar-tab-prepare-v1";

    private final BarTabRepository tabRepository;
    private final BarTabLineRepository lineRepository;
    private final CatalogEntryRepository catalogEntryRepository;
    private final CheckoutSessionRepository checkoutRepository;
    private final BarTabCreationService creationService;
    private final BarTabNormalizer normalizer;
    private final IdempotencyFingerprintService fingerprintService;
    private final CheckoutProperties checkoutProperties;
    private final Clock clock;

    public OperationResult create(
            UUID idempotencyKey,
            CreateBarTabRequest request,
            VitrineUserPrincipal principal
    ) {
        BarTabNormalizer.NormalizedName name =
                normalizer.normalizeName(request.name());

        String fingerprint = fingerprintService.sha256(
                CREATE_VERSION
                        + "|name="
                        + name.normalizedName()
        );

        BarTabEntity existing = tabRepository
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
            BarTabEntity created = creationService.create(
                    name.name(),
                    name.normalizedName(),
                    idempotencyKey,
                    fingerprint,
                    principal.getId()
            );


            return new OperationResult(
                    buildResponse(created),
                    false
            );
        } catch (DataIntegrityViolationException exception) {
            BarTabEntity concurrent = tabRepository
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
    public PageResponse<BarTabResponse> list(
            int page,
            int size,
            BarTabStatus status,
            String search
    ) {
        String normalizedSearch =
                search == null || search.isBlank()
                        ? null
                        : normalizer
                        .normalizeName(search)
                        .normalizedName();

        Specification<BarTabEntity> specification =
                Specification.allOf(
                        BarTabSpecifications.hasStatus(status),
                        BarTabSpecifications.matchesSearch(
                                normalizedSearch
                        )
                );

        Page<BarTabEntity> result = tabRepository.findAll(
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
    public BarTabResponse findById(Long tabId) {
        return buildResponse(getTab(tabId));
    }

    @Transactional
    public BarTabResponse rename(
            Long tabId,
            RenameBarTabRequest request
    ) {
        BarTabEntity tab = getTabForUpdate(tabId);

        BarTabNormalizer.NormalizedName name =
                normalizer.normalizeName(request.name());

        tab.rename(name.name(), name.normalizedName());

        return buildResponse(tab);
    }

    @Transactional
    public BarTabResponse upsertCatalogEntry(
            Long tabId,
            Long catalogEntryId,
            UpsertBarTabLineRequest request
    ) {
        CatalogEntryEntity entry =
                getAvailableCatalogEntry(
                        catalogEntryId
                );

        return upsertResolvedCatalogEntry(
                tabId,
                entry,
                request
        );
    }

    @Transactional
    public BarTabResponse removeCatalogEntry(
            Long tabId,
            Long catalogEntryId
    ) {
        BarTabEntity tab =
                getEditableTab(tabId);

        if (lineRepository
                .deleteByTabIdAndCatalogEntryId(
                        tabId,
                        catalogEntryId
                ) == 0) {

            throw lineNotFound();
        }

        lineRepository.flush();
        refreshDraftTotal(tab);

        return buildResponse(tab);
    }

    private BarTabResponse upsertResolvedCatalogEntry(
            Long tabId,
            CatalogEntryEntity entry,
            UpsertBarTabLineRequest request
    ) {
        BarTabEntity tab =
                getEditableTab(tabId);

        BarTabLineEntity line =
                lineRepository
                        .findByTabIdAndCatalogEntryId(
                                tabId,
                                entry.getId()
                        )
                        .orElse(null);

        if (line == null) {
            line = BarTabLineEntity.catalogEntry(
                    tabId,
                    entry,
                    request.quantity(),
                    request.unitPriceCents()
            );
        } else {
            line.refreshCatalogEntry(
                    entry,
                    request.quantity(),
                    request.unitPriceCents()
            );
        }

        lineRepository.saveAndFlush(line);
        refreshDraftTotal(tab);

        return buildResponse(tab);
    }

    @Transactional
    public OperationResult prepare(
            Long tabId,
            UUID idempotencyKey,
            PrepareBarTabRequest request,
            VitrineUserPrincipal principal
    ) {
        BarTabNormalizer.NormalizedPreparation preparation =
                normalizer.normalizePreparation(
                        request.documentType(),
                        request.cpf(),
                        request.discountCents()
                );

        String fingerprint = fingerprintService.sha256(
                PREPARE_VERSION
                        + "|tabId="
                        + tabId
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

        BarTabEntity tab = getTabForUpdate(tabId);

        if (tab.isPrepared()) {
            validatePrepareReplay(tab, idempotencyKey, fingerprint);

            return new OperationResult(buildResponse(tab), true);
        }

        tab.requireOpen();

        List<BarTabLineEntity> lines =
                lineRepository.findAllByTabIdOrderByIdAsc(tabId);

        if (lines.isEmpty()) {
            throw new BusinessException(
                    "BAR_TAB_EMPTY",
                    "Adicione pelo menos um item a comanda."
            );
        }

        long subtotal = 0L;

        try {
            for (BarTabLineEntity line : lines) {
                line.recalculate();

                subtotal = Math.addExact(
                        subtotal,
                        line.getLineTotalCents()
                );
            }
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                    "BAR_TAB_AMOUNT_OVERFLOW",
                    "O total da comanda excede o limite permitido."
            );
        }

        if (subtotal <= 0) {
            throw new BusinessException(
                    "INVALID_BAR_TAB_TOTAL",
                    "O total da comanda deve ser maior que zero."
            );
        }

        if (preparation.discountCents() >= subtotal) {
            throw new BusinessException(
                    "INVALID_CHECKOUT_DISCOUNT",
                    "O desconto deve ser menor que o subtotal da comanda."
            );
        }

        checkoutRepository
                .findByIdempotencyKey(idempotencyKey)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "IDEMPOTENCY_KEY_ALREADY_USED",
                            "A chave de idempotencia ja foi utilizada."
                    );
                });

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(
                checkoutProperties.getDraftExpiration()
        );

        CheckoutSessionEntity checkout =
                CheckoutSessionEntity.openDraft(
                        idempotencyKey,
                        fingerprint,
                        CheckoutOperationType.BAR_COMMAND,
                        principal.getId(),
                        expiresAt
                );

        checkoutRepository.saveAndFlush(checkout);
        checkout.attachSource(tab.getId());

        checkout.prepareForPayment(
                subtotal,
                preparation.discountCents(),
                preparation.documentType(),
                preparation.cpfDigits(),
                expiresAt
        );

        tab.markPaymentPending(
                checkout.getId(),
                subtotal,
                preparation.discountCents(),
                idempotencyKey,
                fingerprint,
                now
        );

        lineRepository.flush();
        tabRepository.flush();
        checkoutRepository.flush();


        return new OperationResult(buildResponse(tab), false);
    }

    private CatalogEntryEntity getAvailableCatalogEntry(
            Long catalogEntryId
    ) {
        return catalogEntryRepository
                .findByIdAndDeletedAtIsNull(
                        catalogEntryId
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "CATALOG_ENTRY_NOT_FOUND",
                                "Item ou serviço não encontrado."
                        )
                );
    }

    private BarTabEntity getTab(Long tabId) {
        return tabRepository.findById(tabId)
                .orElseThrow(() -> new NotFoundException(
                        "BAR_TAB_NOT_FOUND",
                        "Comanda nao encontrada."
                ));
    }

    private BarTabEntity getTabForUpdate(Long tabId) {
        return tabRepository
                .findByIdForUpdate(tabId)
                .orElseThrow(() -> new NotFoundException(
                        "BAR_TAB_NOT_FOUND",
                        "Comanda nao encontrada."
                ));
    }

    private BarTabEntity getEditableTab(Long tabId) {
        BarTabEntity tab = getTabForUpdate(tabId);
        tab.requireOpen();
        return tab;
    }

    private void refreshDraftTotal(BarTabEntity tab) {
        long subtotal = lineRepository
                .findAllByTabIdOrderByIdAsc(tab.getId())
                .stream()
                .mapToLong(BarTabLineEntity::getLineTotalCents)
                .reduce(0L, Math::addExact);

        tab.updateDraftTotal(subtotal);
    }

    private BarTabResponse buildResponse(BarTabEntity tab) {
        CheckoutSessionEntity checkout =
                tab.getCheckoutSessionId() == null
                        ? null
                        : checkoutRepository
                        .findById(tab.getCheckoutSessionId())
                        .orElse(null);

        List<BarTabLineResponse> lines =
                lineRepository
                        .findAllByTabIdOrderByIdAsc(tab.getId())
                        .stream()
                        .map(BarTabLineResponse::from)
                        .toList();

        return new BarTabResponse(
                tab.getId(),
                tab.getName(),
                tab.getStatus().name(),
                tab.getCheckoutSessionId(),
                checkout == null ? null : checkout.getStatus().name(),
                tab.getSubtotalCents(),
                tab.getDiscountCents(),
                tab.getTotalCents(),
                checkout == null || checkout.getDocumentType() == null
                        ? null
                        : checkout.getDocumentType().name(),
                checkout == null ? null : checkout.getCpfDigits(),
                tab.isPrepared(),
                tab.getPreparedAt(),
                lines,
                tab.getCreatedByUserId(),
                tab.getCreatedAt(),
                tab.getUpdatedAt()
        );
    }

    private void validateCreateReplay(
            BarTabEntity existing,
            String fingerprint,
            Long actorUserId
    ) {
        if (!existing.getCreatedByUserId().equals(actorUserId)) {
            throw new BusinessException(
                    "BAR_TAB_IDEMPOTENCY_KEY_ALREADY_USED",
                    "A chave de idempotencia ja foi utilizada."
            );
        }

        if (!existing.getCreateRequestFingerprint()
                .trim()
                .equals(fingerprint)) {

            throw new BusinessException(
                    "BAR_TAB_IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                    "A chave foi reutilizada com uma requisicao diferente."
            );
        }
    }

    private void validatePrepareReplay(
            BarTabEntity tab,
            UUID idempotencyKey,
            String fingerprint
    ) {
        if (!tab.getPrepareIdempotencyKey().equals(idempotencyKey)) {
            throw new BusinessException(
                    "BAR_TAB_ALREADY_PREPARED",
                    "A comanda ja foi enviada para pagamento."
            );
        }

        if (!tab.getPrepareRequestFingerprint()
                .trim()
                .equals(fingerprint)) {

            throw new BusinessException(
                    "BAR_TAB_PREPARE_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                    "A chave foi reutilizada com uma preparacao diferente."
            );
        }
    }

    private NotFoundException lineNotFound() {
        return new NotFoundException(
                "BAR_TAB_LINE_NOT_FOUND",
                "O item nao esta presente na comanda."
        );
    }

    public record OperationResult(
            BarTabResponse response,
            boolean replayed
    ) {
    }
}
