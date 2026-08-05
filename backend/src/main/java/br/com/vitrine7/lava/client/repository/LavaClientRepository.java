package br.com.vitrine7.lava.client.repository;

import br.com.vitrine7.lava.client.entity.LavaClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LavaClientRepository extends
        JpaRepository<LavaClientEntity, Long>,
        JpaSpecificationExecutor<LavaClientEntity> {

    Optional<LavaClientEntity> findByIdAndDeletedAtIsNull(
            Long id
    );

    boolean existsByPlateAndDeletedAtIsNull(
            String plate
    );

    boolean existsByPlateAndIdNotAndDeletedAtIsNull(
            String plate,
            Long id
    );
}
