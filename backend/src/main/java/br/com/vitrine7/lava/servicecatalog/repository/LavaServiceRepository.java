package br.com.vitrine7.lava.servicecatalog.repository;

import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LavaServiceRepository extends
        JpaRepository<LavaServiceEntity, Long>,
        JpaSpecificationExecutor<LavaServiceEntity> {

    Optional<LavaServiceEntity> findByIdAndDeletedAtIsNull(
            Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT service
            FROM LavaServiceEntity service
            WHERE service.id = :id
              AND service.deletedAt IS NULL
            """)
    Optional<LavaServiceEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT service
            FROM LavaServiceEntity service
            WHERE service.id IN :ids
              AND service.deletedAt IS NULL
            ORDER BY service.id ASC
            """)
    List<LavaServiceEntity> findAllByIdsForUpdate(
            @Param("ids") List<Long> ids
    );

    boolean existsByNormalizedNameAndDeletedAtIsNull(
            String normalizedName
    );

    boolean existsByNormalizedNameAndIdNotAndDeletedAtIsNull(
            String normalizedName,
            Long id
    );
}
