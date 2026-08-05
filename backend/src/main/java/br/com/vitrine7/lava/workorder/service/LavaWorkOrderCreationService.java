package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;
import br.com.vitrine7.lava.servicecatalog.repository.LavaServiceRepository;
import br.com.vitrine7.lava.workorder.entity.LavaVehicleSize;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderEntity;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderLineEntity;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderLineRepository;
import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LavaWorkOrderCreationService {

    private final LavaWorkOrderRepository workOrderRepository;
    private final LavaWorkOrderLineRepository lineRepository;
    private final LavaServiceRepository serviceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LavaWorkOrderEntity create(
            LavaWorkOrderNormalizer.NormalizedCustomer customer,
            LavaVehicleSize vehicleSize,
            Long serviceId,
            UUID idempotencyKey,
            String requestFingerprint,
            Long actorUserId
    ) {
        LavaServiceEntity service =
                serviceRepository.findByIdForUpdate(serviceId)
                        .orElseThrow(() -> new NotFoundException(
                                "LAVA_SERVICE_NOT_FOUND",
                                "Servico do lava jato nao encontrado."
                        ));

        if (service.isDeleted() || !service.isActive()) {
            throw new BusinessException(
                    "LAVA_SERVICE_INACTIVE",
                    "O servico selecionado esta inativo."
            );
        }

        LavaWorkOrderEntity workOrder =
                workOrderRepository.saveAndFlush(
                        LavaWorkOrderEntity.open(
                                customer.clientId(),
                                customer.customerName(),
                                customer.normalizedCustomerName(),
                                customer.phoneDigits(),
                                customer.vehicleName(),
                                customer.normalizedVehicleName(),
                                customer.plate(),
                                vehicleSize,
                                idempotencyKey,
                                requestFingerprint,
                                actorUserId
                        )
                );

        LavaWorkOrderLineEntity line =
                LavaWorkOrderLineEntity.create(
                        workOrder.getId(),
                        service,
                        vehicleSize
                );

        lineRepository.saveAndFlush(line);

        workOrder.updateOpenTotal(
                line.getPriceCents()
        );

        workOrderRepository.flush();

        return workOrder;
    }
}
