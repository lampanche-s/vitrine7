package br.com.vitrine7.lava.servicecatalog.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.lava.servicecatalog.dto.ChangeLavaServiceStatusRequest;
import br.com.vitrine7.lava.servicecatalog.dto.CreateLavaServiceRequest;
import br.com.vitrine7.lava.servicecatalog.dto.LavaServiceResponse;
import br.com.vitrine7.lava.servicecatalog.dto.UpdateLavaServiceRequest;
import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;
import br.com.vitrine7.lava.servicecatalog.repository.LavaServiceRepository;
import br.com.vitrine7.lava.servicecatalog.specification.LavaServiceSpecifications;
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
public class LavaServiceService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "name",
                    "category",
                    "smallVehiclePriceCents",
                    "mediumVehiclePriceCents",
                    "durationMinutes",
                    "active",
                    "createdAt",
                    "updatedAt"
            );

    private static final Map<String, String> SORT_FIELD_MAPPING =
            Map.of(
                    "name", "name",
                    "category", "category",
                    "smallVehiclePriceCents",
                    "smallVehiclePriceCents",
                    "mediumVehiclePriceCents",
                    "mediumVehiclePriceCents",
                    "durationMinutes", "durationMinutes",
                    "active", "active",
                    "createdAt", "createdAt",
                    "updatedAt", "updatedAt"
            );

    private final LavaServiceRepository serviceRepository;
    private final LavaServiceNormalizer serviceNormalizer;

    @Transactional(readOnly = true)
    public PageResponse<LavaServiceResponse> list(
            int page,
            int size,
            String sort,
            String direction,
            String search,
            String category,
            Boolean active
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

        String normalizedSearch =
                serviceNormalizer.normalizeFilter(search);

        String normalizedCategory =
                serviceNormalizer.normalizeFilter(category);

        Specification<LavaServiceEntity> specification =
                Specification.allOf(
                        LavaServiceSpecifications.notDeleted(),
                        LavaServiceSpecifications.matchesSearch(
                                normalizedSearch
                        ),
                        LavaServiceSpecifications.hasCategory(
                                normalizedCategory
                        ),
                        LavaServiceSpecifications.hasActive(
                                active
                        )
                );

        Page<LavaServiceEntity> services =
                serviceRepository.findAll(
                        specification,
                        pageRequest
                );

        return PageResponse.from(
                services,
                LavaServiceResponse::from
        );
    }

    @Transactional(readOnly = true)
    public LavaServiceResponse findById(Long id) {
        return LavaServiceResponse.from(
                getExistingService(id)
        );
    }

    @Transactional
    public LavaServiceResponse create(
            CreateLavaServiceRequest request
    ) {
        LavaServiceNormalizer.NormalizedLavaServiceData data =
                serviceNormalizer.normalize(
                        request.name(),
                        request.category()
                );

        validateUniqueName(
                data.normalizedName(),
                null
        );

        LavaServiceEntity service =
                LavaServiceEntity.create(
                        data.name(),
                        data.normalizedName(),
                        data.category(),
                        data.normalizedCategory(),
                        request.smallVehiclePriceCents(),
                        request.mediumVehiclePriceCents(),
                        request.durationMinutes()
                );

        return LavaServiceResponse.from(
                serviceRepository.save(service)
        );
    }

    @Transactional
    public LavaServiceResponse update(
            Long id,
            UpdateLavaServiceRequest request
    ) {
        LavaServiceEntity service =
                getExistingService(id);

        LavaServiceNormalizer.NormalizedLavaServiceData data =
                serviceNormalizer.normalize(
                        request.name(),
                        request.category()
                );

        validateUniqueName(
                data.normalizedName(),
                service.getId()
        );

        service.update(
                data.name(),
                data.normalizedName(),
                data.category(),
                data.normalizedCategory(),
                request.smallVehiclePriceCents(),
                request.mediumVehiclePriceCents(),
                request.durationMinutes()
        );

        return LavaServiceResponse.from(service);
    }

    @Transactional
    public LavaServiceResponse changeActive(
            Long id,
            ChangeLavaServiceStatusRequest request
    ) {
        LavaServiceEntity service =
                getExistingService(id);

        service.changeActive(request.active());

        return LavaServiceResponse.from(service);
    }

    @Transactional
    public void delete(
            Long id,
            Long actorUserId
    ) {
        LavaServiceEntity service =
                getExistingService(id);

        service.softDelete(actorUserId);
    }

    private LavaServiceEntity getExistingService(Long id) {
        return serviceRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "LAVA_SERVICE_NOT_FOUND",
                        "Serviço não encontrado."
                ));
    }

    private void validateUniqueName(
            String normalizedName,
            Long ignoredServiceId
    ) {
        boolean exists = ignoredServiceId == null
                ? serviceRepository
                .existsByNormalizedNameAndDeletedAtIsNull(
                        normalizedName
                )
                : serviceRepository
                .existsByNormalizedNameAndIdNotAndDeletedAtIsNull(
                        normalizedName,
                        ignoredServiceId
                );

        if (exists) {
            throw new BusinessException(
                    "LAVA_SERVICE_ALREADY_EXISTS",
                    "Já existe um serviço com esse nome."
            );
        }
    }

    private String parseSortField(String sort) {
        String requested = sort == null || sort.isBlank()
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
