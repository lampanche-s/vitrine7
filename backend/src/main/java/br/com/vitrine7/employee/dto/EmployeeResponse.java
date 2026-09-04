package br.com.vitrine7.employee.dto;

import br.com.vitrine7.employee.entity.EmployeeEntity;
import java.time.OffsetDateTime;

public record EmployeeResponse(Long id, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static EmployeeResponse from(EmployeeEntity employee) {
        return new EmployeeResponse(employee.getId(), employee.getName(), employee.getCreatedAt(), employee.getUpdatedAt());
    }
}
