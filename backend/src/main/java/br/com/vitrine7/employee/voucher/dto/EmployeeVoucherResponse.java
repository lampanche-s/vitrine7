package br.com.vitrine7.employee.voucher.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EmployeeVoucherResponse(
        long operationId,
        long employeeId,
        String employeeName,
        String registeredTabName,
        OffsetDateTime closedAt,
        long totalCents,
        int lineCount,
        int totalUnits,
        long operatorUserId,
        String operatorName,
        List<Line> lines
) {
    public record Line(long catalogEntryId, String itemName, String type, int quantity,
                       long unitPriceCents, long lineTotalCents) {}
}
