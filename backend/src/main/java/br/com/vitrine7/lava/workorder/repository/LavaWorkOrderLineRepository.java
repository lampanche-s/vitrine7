package br.com.vitrine7.lava.workorder.repository;

import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface LavaWorkOrderLineRepository
        extends JpaRepository<LavaWorkOrderLineEntity, Long> {

    Optional<LavaWorkOrderLineEntity>
    findByWorkOrderIdAndServiceId(Long workOrderId, Long serviceId);

    List<LavaWorkOrderLineEntity>
    findAllByWorkOrderIdOrderByIdAsc(Long workOrderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int deleteByWorkOrderIdAndServiceId(Long workOrderId, Long serviceId);
}
