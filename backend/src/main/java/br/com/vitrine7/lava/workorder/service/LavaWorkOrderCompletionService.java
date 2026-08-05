package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.lava.workorder.dto.LavaWorkOrderResponse;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LavaWorkOrderCompletionService {

    private final LavaWorkOrderRepository workOrderRepository;
    private final LavaWorkOrderService workOrderService;
    private final Clock clock;

    @Transactional
    public CompletionResult complete(
            Long workOrderId,
            VitrineUserPrincipal principal
    ) {
        LavaWorkOrderEntity workOrder =
                workOrderRepository
                        .findByIdForUpdate(workOrderId)
                        .orElseThrow(() -> new NotFoundException(
                                "LAVA_WORK_ORDER_NOT_FOUND",
                                "Ordem de servico nao encontrada."
                        ));

        boolean replayed =
                workOrder.getStatus()
                        == LavaWorkOrderStatus.COMPLETED;

        if (!replayed) {
            workOrder.complete(
                    principal.getId(),
                    OffsetDateTime.now(clock)
            );

        }

        workOrderRepository.flush();

        return new CompletionResult(
                workOrderService.buildResponse(workOrder),
                replayed
        );
    }

    public record CompletionResult(
            LavaWorkOrderResponse response,
            boolean replayed
    ) {
    }
}
