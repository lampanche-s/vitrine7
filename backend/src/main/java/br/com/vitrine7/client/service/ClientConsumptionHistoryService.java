package br.com.vitrine7.client.service;

import br.com.vitrine7.client.dto.ClientConsumptionHistoryResponse;
import br.com.vitrine7.client.repository.ClientConsumptionHistoryRepository;
import br.com.vitrine7.client.repository.ClientRepository;
import br.com.vitrine7.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientConsumptionHistoryService {

    private final ClientRepository clientRepository;
    private final ClientConsumptionHistoryRepository repository;

    @Transactional(readOnly = true)
    public List<ClientConsumptionHistoryResponse> list(long clientId) {
        if (clientRepository.findByIdAndDeletedAtIsNull(clientId).isEmpty()) {
            throw new NotFoundException(
                    "CLIENT_NOT_FOUND",
                    "Cliente não encontrado."
            );
        }

        Map<Long, MutableHistory> grouped = new LinkedHashMap<>();
        for (ClientConsumptionHistoryRepository.Row row : repository.findByClientId(clientId)) {
            MutableHistory history = grouped.computeIfAbsent(
                    row.operationId(),
                    ignored -> new MutableHistory(row)
            );
            history.lines.add(new ClientConsumptionHistoryResponse.Line(
                    row.entryType(),
                    row.itemName(),
                    row.quantity(),
                    row.unitPriceCents(),
                    row.lineTotalCents()
            ));
        }

        return grouped.values().stream()
                .map(MutableHistory::toResponse)
                .toList();
    }

    private static final class MutableHistory {
        private final ClientConsumptionHistoryRepository.Row row;
        private final List<ClientConsumptionHistoryResponse.Line> lines = new ArrayList<>();

        private MutableHistory(ClientConsumptionHistoryRepository.Row row) {
            this.row = row;
        }

        private ClientConsumptionHistoryResponse toResponse() {
            return new ClientConsumptionHistoryResponse(
                    row.operationId(),
                    row.completedAt(),
                    row.totalCents(),
                    row.paymentStatus(),
                    List.copyOf(lines)
            );
        }
    }
}
