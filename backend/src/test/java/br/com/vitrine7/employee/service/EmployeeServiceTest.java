package br.com.vitrine7.employee.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.employee.dto.CreateEmployeeRequest;
import br.com.vitrine7.employee.entity.EmployeeEntity;
import br.com.vitrine7.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final EmployeeService service = new EmployeeService(repository,
            Clock.fixed(Instant.parse("2026-08-25T18:30:00Z"), ZoneOffset.UTC));

    @Test
    void createsWithOnlyNormalizedName() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            EmployeeEntity employee = invocation.getArgument(0);
            ReflectionTestUtils.setField(employee, "id", 1L);
            return employee;
        });
        var response = service.create(new CreateEmployeeRequest("  João  da Silva "));
        assertEquals("João da Silva", response.name());
        verify(repository).existsByNormalizedNameAndDeletedAtIsNull("joao da silva");
    }

    @Test
    void rejectsDuplicateNormalizedName() {
        when(repository.existsByNormalizedNameAndDeletedAtIsNull("joao")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(new CreateEmployeeRequest("João")));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(BusinessException.class, () -> service.create(new CreateEmployeeRequest("   ")));
        verify(repository, never()).saveAndFlush(any());
    }
}
