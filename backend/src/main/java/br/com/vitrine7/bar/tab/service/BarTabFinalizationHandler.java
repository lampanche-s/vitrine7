package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.repository.BarTabLineRepository;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.service.CheckoutFinalizationHandler;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BarTabFinalizationHandler
        implements CheckoutFinalizationHandler {

    private final BarTabRepository tabRepository;
    private final BarTabLineRepository lineRepository;
    private final CatalogEntryRepository catalogEntryRepository;

    @Override
    public boolean supports(
            CheckoutOperationType operationType
    ) {
        return operationType
                == CheckoutOperationType.BAR_COMMAND;
    }

    @Override
    public int finalizeOperation(
            CheckoutSessionEntity checkout,
            Long actorUserId,
            OffsetDateTime finalizedAt
    ) {
        BarTabEntity tab =
                tabRepository
                        .findByCheckoutSessionIdForUpdate(
                                checkout.getId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "BAR_TAB_NOT_FOUND",
                                        "Comanda não encontrada."
                                )
                        );

        validateCheckoutLink(
                tab,
                checkout
        );

        List<BarTabLineEntity> lines =
                lineRepository
                        .findAllByTabIdOrderByIdAsc(
                                tab.getId()
                        );

        if (lines.isEmpty()) {
            throw new BusinessException(
                    "BAR_TAB_EMPTY",
                    "A comanda não possui itens ou serviços."
            );
        }

        List<Long> catalogEntryIds = lines.stream()
                .map(BarTabLineEntity::getCatalogEntryId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, CatalogEntryEntity> entriesById =
                catalogEntryRepository
                        .findAllAvailableByIdForUpdate(
                                catalogEntryIds
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                CatalogEntryEntity::getId,
                                Function.identity()
                        ));

        for (BarTabLineEntity line : lines) {
            CatalogEntryEntity entry = entriesById.get(
                    line.getCatalogEntryId()
            );

            if (entry == null) {
                throw new BusinessException(
                        "CATALOG_ENTRY_NOT_FOUND",
                        "Um item ou serviço da comanda não está mais disponível."
                );
            }

            validateAvailableStock(
                    entry,
                    line.getQuantity()
            );
        }

        for (BarTabLineEntity line : lines) {
            CatalogEntryEntity entry = entriesById.get(
                    line.getCatalogEntryId()
            );

            if (entry.tracksStock()) {
                entry.decreaseStock(
                        line.getQuantity()
                );
            }
        }

        catalogEntryRepository.flush();

        tab.markClosed(
                checkout.getId(),
                finalizedAt
        );

        tabRepository.flush();

        return lines.size();
    }

    private void validateAvailableStock(
            CatalogEntryEntity entry,
            int requestedQuantity
    ) {
        if (!entry.tracksStock()) {
            return;
        }

        int available = entry.getStockQuantity() == null
                ? 0
                : entry.getStockQuantity();

        if (available == 0) {
            throw new BusinessException(
                    "CATALOG_ENTRY_OUT_OF_STOCK",
                    "O item " + entry.getName() + " está sem estoque."
            );
        }

        if (!entry.hasAvailableStock(requestedQuantity)) {
            throw new BusinessException(
                    "CATALOG_ENTRY_INSUFFICIENT_STOCK",
                    "Estoque insuficiente para "
                            + entry.getName()
                            + ". Disponível: "
                            + available
                            + "."
            );
        }
    }

    private void validateCheckoutLink(
            BarTabEntity tab,
            CheckoutSessionEntity checkout
    ) {
        if (tab.getStatus()
                != BarTabStatus.PAYMENT_PENDING) {

            throw new BusinessException(
                    "BAR_TAB_NOT_PAYMENT_PENDING",
                    "A comanda não está aguardando pagamento."
            );
        }

        if (tab.getCheckoutSessionId() == null
                || !tab.getCheckoutSessionId()
                .equals(checkout.getId())
                || checkout.getSourceId() == null
                || !checkout.getSourceId()
                .equals(tab.getId())) {

            throw new BusinessException(
                    "BAR_TAB_CHECKOUT_LINK_INVALID",
                    "O vínculo entre a comanda e o checkout é inválido."
            );
        }
    }
}
