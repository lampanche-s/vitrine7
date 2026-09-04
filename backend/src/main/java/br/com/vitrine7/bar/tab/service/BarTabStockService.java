package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BarTabStockService {
    private final CatalogEntryRepository catalogEntryRepository;

    public void validateAndDecrease(List<BarTabLineEntity> lines) {
        Map<Long, CatalogEntryEntity> entries = lockedEntries(lines, true);
        for (BarTabLineEntity line : lines) validate(entries.get(line.getCatalogEntryId()), line);
        for (BarTabLineEntity line : lines) {
            CatalogEntryEntity entry = entries.get(line.getCatalogEntryId());
            if (entry.tracksStock()) entry.decreaseStock(line.getQuantity());
        }
        catalogEntryRepository.flush();
    }

    public void restore(List<BarTabLineEntity> lines) {
        Map<Long, CatalogEntryEntity> entries = lockedEntries(lines, false);
        for (BarTabLineEntity line : lines) {
            CatalogEntryEntity entry = entries.get(line.getCatalogEntryId());
            if (entry.tracksStock()) entry.increaseStock(line.getQuantity());
        }
        catalogEntryRepository.flush();
    }

    private Map<Long, CatalogEntryEntity> lockedEntries(List<BarTabLineEntity> lines, boolean availableOnly) {
        List<Long> ids = lines.stream().map(BarTabLineEntity::getCatalogEntryId).distinct().sorted().toList();
        Map<Long, CatalogEntryEntity> result = (availableOnly ? catalogEntryRepository.findAllAvailableByIdForUpdate(ids) : catalogEntryRepository.findAllByIdForUpdate(ids)).stream().collect(Collectors.toMap(CatalogEntryEntity::getId, Function.identity()));
        if (result.size() != lines.stream().map(BarTabLineEntity::getCatalogEntryId).distinct().count()) {
            throw new BusinessException("CATALOG_ENTRY_NOT_FOUND", "Um item ou serviço da comanda não está mais disponível.");
        }
        return result;
    }

    private void validate(CatalogEntryEntity entry, BarTabLineEntity line) {
        if (!entry.tracksStock()) return;
        int available = entry.getStockQuantity() == null ? 0 : entry.getStockQuantity();
        if (available == 0) throw new BusinessException("CATALOG_ENTRY_OUT_OF_STOCK", "O item " + entry.getName() + " está sem estoque.");
        if (!entry.hasAvailableStock(line.getQuantity())) throw new BusinessException("CATALOG_ENTRY_INSUFFICIENT_STOCK", "Estoque insuficiente para " + entry.getName() + ". Disponível: " + available + ".");
    }
}
