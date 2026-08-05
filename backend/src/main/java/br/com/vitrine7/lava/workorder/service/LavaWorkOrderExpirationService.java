package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.lava.workorder.repository.LavaWorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LavaWorkOrderExpirationService {

    private final LavaWorkOrderRepository workOrderRepository;

    @Transactional
    public int reopenExpiredWorkOrders() {
        return workOrderRepository
                .reopenReleasedOrExpiredWorkOrders();
    }
}
