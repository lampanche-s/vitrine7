package br.com.vitrine7.employee.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.employee.dto.*;
import br.com.vitrine7.employee.entity.EmployeeEntity;
import br.com.vitrine7.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(int page, int size) {
        return PageResponse.from(repository.findAllByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))), EmployeeResponse::from);
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Name name = normalize(request.name());
        if (repository.existsByNormalizedNameAndDeletedAtIsNull(name.normalized())) throw duplicated();
        try {
            return EmployeeResponse.from(repository.saveAndFlush(EmployeeEntity.create(name.value(), name.normalized())));
        } catch (DataIntegrityViolationException exception) {
            throw duplicated();
        }
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        EmployeeEntity employee = available(id);
        Name name = normalize(request.name());
        if (repository.existsByNormalizedNameAndIdNotAndDeletedAtIsNull(name.normalized(), id)) throw duplicated();
        employee.update(name.value(), name.normalized());
        try { repository.flush(); } catch (DataIntegrityViolationException exception) { throw duplicated(); }
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public void delete(Long id, Long actorUserId) {
        available(id).softDelete(actorUserId, OffsetDateTime.now(clock));
    }

    private EmployeeEntity available(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id).orElseThrow(() ->
                new NotFoundException("EMPLOYEE_NOT_FOUND", "Funcionário não encontrado."));
    }

    private Name normalize(String raw) {
        String value = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            throw new BusinessException("EMPLOYEE_NAME_REQUIRED", "Informe o nome do funcionário.");
        }
        if (value.length() > 80) {
            throw new BusinessException("EMPLOYEE_NAME_TOO_LONG", "O nome deve possuir no máximo 80 caracteres.");
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return new Name(value, normalized);
    }

    private BusinessException duplicated() {
        return new BusinessException("EMPLOYEE_NAME_ALREADY_EXISTS", "Já existe um funcionário cadastrado com este nome.");
    }

    private record Name(String value, String normalized) {}
}
