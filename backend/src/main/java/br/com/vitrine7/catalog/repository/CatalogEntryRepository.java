package br.com.vitrine7.catalog.repository;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CatalogEntryRepository extends
        JpaRepository<CatalogEntryEntity, Long>,
        JpaSpecificationExecutor<CatalogEntryEntity> {

    Optional<CatalogEntryEntity>
    findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT entry
            FROM CatalogEntryEntity entry
            WHERE entry.id IN :ids
              AND entry.deletedAt IS NULL
            ORDER BY entry.id
            """)
    List<CatalogEntryEntity> findAllAvailableByIdForUpdate(
            @Param("ids") Collection<Long> ids
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT entry
            FROM CatalogEntryEntity entry
            WHERE entry.id IN :ids
            ORDER BY entry.id
            """)
    List<CatalogEntryEntity> findAllByIdForUpdate(
            @Param("ids") Collection<Long> ids
    );

    List<CatalogEntryEntity> findAllBySupplierIdAndEntryTypeAndDeletedAtIsNullOrderByNameAsc(
            Long supplierId,
            CatalogEntryType entryType
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE CatalogEntryEntity entry
            SET entry.supplier = NULL
            WHERE entry.supplier.id = :supplierId
            """)
    int clearSupplierBySupplierId(@Param("supplierId") Long supplierId);

    Optional<CatalogEntryEntity>
    findByEntryTypeAndNormalizedNameAndDeletedAtIsNull(
            CatalogEntryType entryType,
            String normalizedName
    );

    boolean
    existsByEntryTypeAndNormalizedNameAndDeletedAtIsNull(
            CatalogEntryType entryType,
            String normalizedName
    );

    boolean
    existsByEntryTypeAndNormalizedNameAndIdNotAndDeletedAtIsNull(
            CatalogEntryType entryType,
            String normalizedName,
            Long id
    );
}
