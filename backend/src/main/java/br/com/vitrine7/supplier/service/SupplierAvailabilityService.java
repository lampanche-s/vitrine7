package br.com.vitrine7.supplier.service;

import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.supplier.entity.SupplierEntity;
import br.com.vitrine7.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplierAvailabilityService {
    private final SupplierRepository repository;

    public SupplierEntity lockAvailable(Long id) {
        return repository.findAvailableByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(
                        "SUPPLIER_NOT_FOUND",
                        "Fornecedor não encontrado."
                ));
    }
}
