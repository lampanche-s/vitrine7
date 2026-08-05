package br.com.vitrine7.lava.client.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.lava.client.dto.ChangeLavaClientStatusRequest;
import br.com.vitrine7.lava.client.dto.CreateLavaClientRequest;
import br.com.vitrine7.lava.client.dto.LavaClientResponse;
import br.com.vitrine7.lava.client.dto.UpdateLavaClientRequest;
import br.com.vitrine7.lava.client.entity.LavaClientEntity;
import br.com.vitrine7.lava.client.repository.LavaClientRepository;
import br.com.vitrine7.lava.client.specification.LavaClientSpecifications;
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
public class LavaClientService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "name",
                    "phone",
                    "vehicleName",
                    "plate",
                    "visitsCount",
                    "lastServiceAt",
                    "active",
                    "createdAt",
                    "updatedAt"
            );

    private static final Map<String, String> SORT_FIELD_MAPPING =
            Map.of(
                    "name", "name",
                    "phone", "phoneDigits",
                    "vehicleName", "vehicleName",
                    "plate", "plate",
                    "visitsCount", "visitsCount",
                    "lastServiceAt", "lastServiceAt",
                    "active", "active",
                    "createdAt", "createdAt",
                    "updatedAt", "updatedAt"
            );

    private final LavaClientRepository clientRepository;
    private final LavaClientNormalizer clientNormalizer;

    @Transactional(readOnly = true)
    public PageResponse<LavaClientResponse> list(
            int page,
            int size,
            String sort,
            String direction,
            String search,
            Boolean active
    ) {
        String sortField = parseSortField(sort);
        Sort.Direction sortDirection =
                parseDirection(direction);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        sortDirection,
                        SORT_FIELD_MAPPING.get(sortField)
                )
        );

        Specification<LavaClientEntity> specification =
                Specification.allOf(
                        LavaClientSpecifications.notDeleted(),
                        LavaClientSpecifications.matchesSearch(
                                search
                        ),
                        LavaClientSpecifications.hasActive(
                                active
                        )
                );

        Page<LavaClientEntity> clients =
                clientRepository.findAll(
                        specification,
                        pageRequest
                );

        return PageResponse.from(
                clients,
                LavaClientResponse::from
        );
    }

    @Transactional(readOnly = true)
    public LavaClientResponse findById(Long id) {
        return LavaClientResponse.from(
                getExistingClient(id)
        );
    }

    @Transactional
    public LavaClientResponse create(
            CreateLavaClientRequest request
    ) {
        LavaClientNormalizer.NormalizedLavaClientData data =
                clientNormalizer.normalize(
                        request.name(),
                        request.phone(),
                        request.vehicleName(),
                        request.plate()
                );

        validateUniquePlate(
                data.plate(),
                null
        );

        LavaClientEntity client =
                LavaClientEntity.create(
                        data.name(),
                        data.normalizedName(),
                        data.phoneDigits(),
                        data.vehicleName(),
                        data.normalizedVehicleName(),
                        data.plate()
                );

        return LavaClientResponse.from(
                clientRepository.save(client)
        );
    }

    @Transactional
    public LavaClientResponse update(
            Long id,
            UpdateLavaClientRequest request
    ) {
        LavaClientEntity client =
                getExistingClient(id);

        LavaClientNormalizer.NormalizedLavaClientData data =
                clientNormalizer.normalize(
                        request.name(),
                        request.phone(),
                        request.vehicleName(),
                        request.plate()
                );

        validateUniquePlate(
                data.plate(),
                client.getId()
        );

        client.updateRegistrationData(
                data.name(),
                data.normalizedName(),
                data.phoneDigits(),
                data.vehicleName(),
                data.normalizedVehicleName(),
                data.plate()
        );

        return LavaClientResponse.from(client);
    }

    @Transactional
    public LavaClientResponse changeActive(
            Long id,
            ChangeLavaClientStatusRequest request
    ) {
        LavaClientEntity client =
                getExistingClient(id);

        client.changeActive(request.active());

        return LavaClientResponse.from(client);
    }

    @Transactional
    public void delete(
            Long id,
            Long actorUserId
    ) {
        LavaClientEntity client =
                getExistingClient(id);

        client.softDelete(actorUserId);
    }

    private LavaClientEntity getExistingClient(Long id) {
        return clientRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "LAVA_CLIENT_NOT_FOUND",
                        "Cliente não encontrado."
                ));
    }

    private void validateUniquePlate(
            String plate,
            Long ignoredClientId
    ) {
        boolean exists = ignoredClientId == null
                ? clientRepository
                .existsByPlateAndDeletedAtIsNull(plate)
                : clientRepository
                .existsByPlateAndIdNotAndDeletedAtIsNull(
                        plate,
                        ignoredClientId
                );

        if (exists) {
            throw new BusinessException(
                    "CLIENT_PLATE_ALREADY_EXISTS",
                    "Já existe um cliente cadastrado "
                            + "com esta placa."
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
                    "O campo de ordenação informado "
                            + "não é permitido."
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
