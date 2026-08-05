package br.com.vitrine7.bar.tab.repository;

import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarTabLineRepository
        extends JpaRepository<BarTabLineEntity, Long> {

    List<BarTabLineEntity>
    findAllByTabIdOrderByIdAsc(Long tabId);

    Optional<BarTabLineEntity>
    findByTabIdAndCatalogEntryId(
            Long tabId,
            Long catalogEntryId
    );

    int deleteByTabIdAndCatalogEntryId(
            Long tabId,
            Long catalogEntryId
    );

    int deleteAllByTabId(Long tabId);
}
