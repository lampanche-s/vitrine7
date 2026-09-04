package br.com.vitrine7.supplier.service;

import br.com.vitrine7.catalog.entity.CatalogEntryType;
import br.com.vitrine7.catalog.repository.CatalogEntryRepository;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.supplier.dto.CreateSupplierRequest;
import br.com.vitrine7.supplier.dto.SupplierDetailResponse;
import br.com.vitrine7.supplier.dto.SupplierResponse;
import br.com.vitrine7.supplier.dto.UpdateSupplierRequest;
import br.com.vitrine7.supplier.entity.SupplierEntity;
import br.com.vitrine7.supplier.repository.SupplierRepository;
import br.com.vitrine7.supplier.specification.SupplierSpecifications;
import lombok.RequiredArgsConstructor;
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
public class SupplierService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "cnpj", "phone", "cep", "createdAt", "updatedAt");
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of("name", "name", "cnpj", "cnpjDigits", "phone", "phoneDigits", "cep", "cepDigits", "createdAt", "createdAt", "updatedAt", "updatedAt");
    private final SupplierRepository repository;
    private final CatalogEntryRepository catalogRepository;
    private final SupplierNormalizer normalizer;
    private final SupplierAvailabilityService availabilityService;

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> list(int page, int size, String sort, String direction, String search) {
        String field = parseSortField(sort);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(parseDirection(direction), SORT_FIELD_MAPPING.get(field)));
        Specification<SupplierEntity> specification = Specification.allOf(SupplierSpecifications.notDeleted(), SupplierSpecifications.matchesSearch(search));
        Page<SupplierEntity> suppliers = repository.findAll(specification, pageRequest);
        return PageResponse.from(suppliers, SupplierResponse::from);
    }

    @Transactional(readOnly = true)
    public SupplierDetailResponse findById(Long id) {
        SupplierEntity supplier = getExisting(id);
        return SupplierDetailResponse.from(supplier, catalogRepository.findAllBySupplierIdAndEntryTypeAndDeletedAtIsNullOrderByNameAsc(id, CatalogEntryType.ITEM));
    }

    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        SupplierNormalizer.NormalizedSupplierData data = normalizer.normalize(request.name(), request.cnpj(), request.phone(), request.cep());
        return SupplierResponse.from(repository.saveAndFlush(SupplierEntity.create(data.name(), data.normalizedName(), data.cnpjDigits(), data.phoneDigits(), data.cepDigits())));
    }

    @Transactional
    public SupplierResponse update(Long id, UpdateSupplierRequest request) {
        SupplierEntity supplier = getExisting(id);
        SupplierNormalizer.NormalizedSupplierData data = normalizer.normalize(request.name(), request.cnpj(), request.phone(), request.cep());
        supplier.update(data.name(), data.normalizedName(), data.cnpjDigits(), data.phoneDigits(), data.cepDigits());
        return SupplierResponse.from(supplier);
    }

    @Transactional
    public void delete(Long id, Long actorUserId) {
        SupplierEntity supplier = availabilityService.lockAvailable(id);
        catalogRepository.clearSupplierBySupplierId(supplier.getId());
        supplier.softDelete(actorUserId);
    }

    private SupplierEntity getExisting(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("SUPPLIER_NOT_FOUND", "Fornecedor não encontrado."));
    }
    private String parseSortField(String sort) {
        String field = sort == null || sort.isBlank() ? "name" : sort.trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) throw new InvalidRequestException("INVALID_SORT_FIELD", "O campo de ordenação informado não é permitido.");
        return field;
    }
    private Sort.Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) return Sort.Direction.ASC;
        try { return Sort.Direction.fromString(direction); }
        catch (IllegalArgumentException exception) { throw new InvalidRequestException("INVALID_SORT_DIRECTION", "A direção deve ser ASC ou DESC."); }
    }
}
