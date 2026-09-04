package br.com.vitrine7.supplier.repository;

import br.com.vitrine7.supplier.entity.SupplierEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long>, JpaSpecificationExecutor<SupplierEntity> {

    Optional<SupplierEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT supplier FROM SupplierEntity supplier
            WHERE supplier.id = :id AND supplier.deletedAt IS NULL
            """)
    Optional<SupplierEntity> findAvailableByIdForUpdate(@Param("id") Long id);
}
