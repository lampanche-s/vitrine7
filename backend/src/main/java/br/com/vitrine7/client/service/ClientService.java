package br.com.vitrine7.client.service;

import br.com.vitrine7.client.dto.ChangeClientStatusRequest;
import br.com.vitrine7.client.dto.ClientResponse;
import br.com.vitrine7.client.dto.CreateClientRequest;
import br.com.vitrine7.client.dto.UpdateClientRequest;
import br.com.vitrine7.client.entity.ClientEntity;
import br.com.vitrine7.client.repository.ClientRepository;
import br.com.vitrine7.client.specification.ClientSpecifications;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
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
public class ClientService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "phone",
            "vehicleName",
            "plate",
            "active",
            "createdAt",
            "updatedAt"
    );

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            "name", "name",
            "phone", "phoneDigits",
            "vehicleName", "vehicleName",
            "plate", "plate",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final ClientRepository clientRepository;
    private final ClientNormalizer clientNormalizer;

    public ClientService(
            ClientRepository clientRepository,
            ClientNormalizer clientNormalizer
    ) {
        this.clientRepository = clientRepository;
        this.clientNormalizer = clientNormalizer;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> list(
            int page,
            int size,
            String sort,
            String direction,
            String search,
            Boolean active
    ) {
        String sortField = parseSortField(sort);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(parseDirection(direction), SORT_FIELD_MAPPING.get(sortField))
        );

        Specification<ClientEntity> specification = Specification.allOf(
                ClientSpecifications.notDeleted(),
                ClientSpecifications.matchesSearch(search),
                ClientSpecifications.hasActive(active)
        );

        Page<ClientEntity> clients = clientRepository.findAll(specification, pageRequest);
        return PageResponse.from(clients, ClientResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return ClientResponse.from(getExistingClient(id));
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        ClientNormalizer.NormalizedClientData data = clientNormalizer.normalize(
                request.name(),
                request.phone(),
                request.vehicleName(),
                request.plate()
        );

        validateUniquePlate(data.plate(), null);

        ClientEntity client = ClientEntity.create(
                data.name(),
                data.normalizedName(),
                data.phoneDigits(),
                data.vehicleName(),
                data.normalizedVehicleName(),
                data.plate()
        );

        try {
            return ClientResponse.from(
                    clientRepository.saveAndFlush(client)
            );
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedPlate();
        }
    }

    @Transactional
    public ClientResponse update(Long id, UpdateClientRequest request) {
        ClientEntity client = getExistingClient(id);
        ClientNormalizer.NormalizedClientData data = clientNormalizer.normalize(
                request.name(),
                request.phone(),
                request.vehicleName(),
                request.plate()
        );

        validateUniquePlate(data.plate(), client.getId());
        client.update(
                data.name(),
                data.normalizedName(),
                data.phoneDigits(),
                data.vehicleName(),
                data.normalizedVehicleName(),
                data.plate()
        );

        try {
            clientRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedPlate();
        }

        return ClientResponse.from(client);
    }

    @Transactional
    public ClientResponse changeActive(Long id, ChangeClientStatusRequest request) {
        ClientEntity client = getExistingClient(id);
        client.changeActive(request.active());
        return ClientResponse.from(client);
    }

    @Transactional
    public void delete(Long id, Long actorUserId) {
        getExistingClient(id).softDelete(actorUserId);
    }

    private ClientEntity getExistingClient(Long id) {
        return clientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "CLIENT_NOT_FOUND",
                        "Cliente não encontrado."
                ));
    }

    private void validateUniquePlate(String plate, Long ignoredClientId) {
        boolean exists = ignoredClientId == null
                ? clientRepository.existsByPlateAndDeletedAtIsNull(plate)
                : clientRepository.existsByPlateAndIdNotAndDeletedAtIsNull(
                        plate,
                        ignoredClientId
                );

        if (exists) {
            throw duplicatedPlate();
        }
    }

    private BusinessException duplicatedPlate() {
        return new BusinessException(
                "CLIENT_PLATE_ALREADY_EXISTS",
                "Já existe um cliente cadastrado com esta placa."
        );
    }

    private String parseSortField(String sort) {
        String requested = sort == null || sort.isBlank() ? "name" : sort.trim();
        if (!ALLOWED_SORT_FIELDS.contains(requested)) {
            throw new InvalidRequestException(
                    "INVALID_SORT_FIELD",
                    "O campo de ordenação informado não é permitido."
            );
        }
        return requested;
    }

    private Sort.Direction parseDirection(String direction) {
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
