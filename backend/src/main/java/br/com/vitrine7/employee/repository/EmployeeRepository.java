package br.com.vitrine7.employee.repository;

import br.com.vitrine7.employee.entity.EmployeeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findByIdAndDeletedAtIsNull(Long id);
    Page<EmployeeEntity> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByNormalizedNameAndDeletedAtIsNull(String normalizedName);
    boolean existsByNormalizedNameAndIdNotAndDeletedAtIsNull(String normalizedName, Long id);
}
