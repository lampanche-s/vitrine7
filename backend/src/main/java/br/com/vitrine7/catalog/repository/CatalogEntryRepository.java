package br.com.vitrine7.catalog.repository;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CatalogEntryRepository extends
        JpaRepository<CatalogEntryEntity, Long>,
        JpaSpecificationExecutor<CatalogEntryEntity> {

    Optional<CatalogEntryEntity>
    findByIdAndDeletedAtIsNull(Long id);

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
