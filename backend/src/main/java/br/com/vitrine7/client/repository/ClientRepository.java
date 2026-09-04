package br.com.vitrine7.client.repository;

import br.com.vitrine7.client.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ClientRepository extends
        JpaRepository<ClientEntity, Long>,
        JpaSpecificationExecutor<ClientEntity> {

    Optional<ClientEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPlateAndDeletedAtIsNull(String plate);

    boolean existsByPlateAndIdNotAndDeletedAtIsNull(String plate, Long id);
}
