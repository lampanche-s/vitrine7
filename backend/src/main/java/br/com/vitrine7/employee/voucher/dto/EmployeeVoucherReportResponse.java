package br.com.vitrine7.employee.voucher.dto;

import java.time.LocalDate;
import java.util.List;

public record EmployeeVoucherReportResponse(
        Summary summary,
        List<ByEmployee> byEmployee,
        List<ByEntry> byEntry,
        List<ByDay> byDay
) {
    public record Summary(long voucherValueCents, int voucherCount, long averageVoucherCents, int consumedUnits) {}
    public record ByEmployee(long employeeId, String employeeName, int voucherCount, long totalCents, int consumption) {}
    public record ByEntry(long catalogEntryId, String name, String type, int quantity, long totalCents, Double evolutionPercent) {}
    public record ByDay(LocalDate date, int voucherCount, long totalCents) {}
}
