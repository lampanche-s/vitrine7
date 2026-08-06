package br.com.vitrine7.catalog.service;

import br.com.vitrine7.catalog.dto.CatalogEntryResponse;
import br.com.vitrine7.catalog.dto.CreateCatalogEntryRequest;
import br.com.vitrine7.catalog.dto.UpdateCatalogEntryRequest;
import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.catalog.specification.CatalogEntrySpecifications;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogEntryService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "name",
                    "type",
                    "priceCents",
                    "stockQuantity",
                    "minimumStockQuantity",
                    "createdAt",
                    "updatedAt"
            );

    private static final Map<String, String>
            SORT_FIELD_MAPPING = Map.of(
                    "name", "name",
                    "type", "entryType",
                    "priceCents", "priceCents",
                    "stockQuantity", "stockQuantity",
                    "minimumStockQuantity", "minimumStockQuantity",
                    "createdAt", "createdAt",
                    "updatedAt", "updatedAt"
            );

    private final CatalogEntryRepository repository;
    private final CatalogEntryTextNormalizer textNormalizer;

    @Transactional(readOnly = true)
    public PageResponse<CatalogEntryResponse> list(
            int page,
            int size,
            String sort,
            String direction,
            String search,
            CatalogEntryType type
    ) {
        String sortField = parseSortField(sort);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        parseDirection(direction),
                        SORT_FIELD_MAPPING.get(sortField)
                )
        );

        Specification<CatalogEntryEntity> specification =
                Specification.allOf(
                        CatalogEntrySpecifications.notDeleted(),
                        CatalogEntrySpecifications.matchesSearch(search),
                        CatalogEntrySpecifications.hasType(type)
                );

        Page<CatalogEntryEntity> entries =
                repository.findAll(
                        specification,
                        pageRequest
                );

        return PageResponse.from(
                entries,
                CatalogEntryResponse::from
        );
    }

    @Transactional(readOnly = true)
    public CatalogEntryResponse findById(Long id) {
        return CatalogEntryResponse.from(
                getExistingEntry(id)
        );
    }

    @Transactional
    public CatalogEntryResponse create(
            CreateCatalogEntryRequest request
    ) {
        CatalogEntryTextNormalizer
                .NormalizedCatalogEntryName normalized =
                textNormalizer.normalize(request.name());

        validateUniqueName(
                request.type(),
                normalized.normalizedName(),
                null
        );

        StockConfiguration stock = resolveStockConfiguration(
                request.type(),
                request.stockQuantity(),
                request.minimumStockQuantity()
        );

        CatalogEntryEntity entry =
                CatalogEntryEntity.create(
                        request.type(),
                        normalized.name(),
                        normalized.normalizedName(),
                        request.priceCents(),
                        stock.stockQuantity(),
                        stock.minimumStockQuantity()
                );

        try {
            return CatalogEntryResponse.from(
                    repository.saveAndFlush(entry)
            );
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedEntry();
        }
    }

    @Transactional
    public CatalogEntryResponse update(
            Long id,
            UpdateCatalogEntryRequest request
    ) {
        CatalogEntryEntity entry =
                getExistingEntry(id);

        CatalogEntryTextNormalizer
                .NormalizedCatalogEntryName normalized =
                textNormalizer.normalize(request.name());

        validateUniqueName(
                request.type(),
                normalized.normalizedName(),
                id
        );

        StockConfiguration stock = resolveStockConfiguration(
                request.type(),
                request.stockQuantity(),
                request.minimumStockQuantity()
        );

        entry.update(
                request.type(),
                normalized.name(),
                normalized.normalizedName(),
                request.priceCents(),
                stock.stockQuantity(),
                stock.minimumStockQuantity()
        );

        try {
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedEntry();
        }

        return CatalogEntryResponse.from(entry);
    }

    @Transactional
    public void delete(
            Long id,
            Long actorUserId
    ) {
        CatalogEntryEntity entry =
                getExistingEntry(id);

        entry.softDelete(actorUserId);
    }

    private CatalogEntryEntity getExistingEntry(
            Long id
    ) {
        return repository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "CATALOG_ENTRY_NOT_FOUND",
                                "Item ou serviço não encontrado."
                        )
                );
    }

    private void validateUniqueName(
            CatalogEntryType type,
            String normalizedName,
            Long currentId
    ) {
        boolean duplicated =
                currentId == null
                        ? repository
                        .existsByEntryTypeAndNormalizedNameAndDeletedAtIsNull(
                                type,
                                normalizedName
                        )
                        : repository
                        .existsByEntryTypeAndNormalizedNameAndIdNotAndDeletedAtIsNull(
                                type,
                                normalizedName,
                                currentId
                        );

        if (duplicated) {
            throw duplicatedEntry();
        }
    }

    private BusinessException duplicatedEntry() {
        return new BusinessException(
                "CATALOG_ENTRY_ALREADY_EXISTS",
                "Já existe um cadastro disponível com esse tipo e nome."
        );
    }


    private StockConfiguration resolveStockConfiguration(
            CatalogEntryType type,
            Integer stockQuantity,
            Integer minimumStockQuantity
    ) {
        if (type == CatalogEntryType.SERVICE) {
            return new StockConfiguration(null, null);
        }

        if (stockQuantity == null
                || minimumStockQuantity == null) {
            throw new InvalidRequestException(
                    "CATALOG_STOCK_REQUIRED",
                    "Informe o estoque atual e o estoque mínimo do item."
            );
        }

        return new StockConfiguration(
                stockQuantity,
                minimumStockQuantity
        );
    }

    private record StockConfiguration(
            Integer stockQuantity,
            Integer minimumStockQuantity
    ) {
    }

    private String parseSortField(String sort) {
        String requested =
                sort == null || sort.isBlank()
                        ? "name"
                        : sort.trim();

        if (!ALLOWED_SORT_FIELDS.contains(requested)) {
            throw new InvalidRequestException(
                    "INVALID_SORT_FIELD",
                    "O campo de ordenação informado não é permitido."
            );
        }

        return requested;
    }

    private Sort.Direction parseDirection(
            String direction
    ) {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.ASC;
        }

        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "INVALID_SORT_DIRECTION",
                    "A direção deve ser ASC ou DESC."
            );
        }
    }
}
