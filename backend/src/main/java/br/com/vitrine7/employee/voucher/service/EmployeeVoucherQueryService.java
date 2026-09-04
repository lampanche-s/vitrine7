package br.com.vitrine7.employee.voucher.service;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.employee.voucher.dto.*;
import br.com.vitrine7.employee.voucher.repository.EmployeeVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeVoucherQueryService {
    private final EmployeeVoucherRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeVoucherResponse> list(Long employeeId, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        long total = repository.count(employeeId, from, to);
        List<Long> ids = repository.pageIds(employeeId, from, to, size, (long) page * size);
        List<EmployeeVoucherResponse> items = group(repository.rowsByIds(ids));
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(items, page, size, total, totalPages, page == 0, page + 1 >= totalPages);
    }

    @Transactional(readOnly = true)
    public EmployeeVoucherReportResponse report(Long employeeId, OffsetDateTime from, OffsetDateTime to) {
        List<EmployeeVoucherRepository.Row> rows = repository.reportRows(employeeId, from, to);
        Map<Long, EmployeeVoucherResponse> vouchers = new LinkedHashMap<>();
        for (EmployeeVoucherResponse voucher : group(rows)) vouchers.put(voucher.operationId(), voucher);

        long total = vouchers.values().stream().mapToLong(EmployeeVoucherResponse::totalCents).sum();
        int units = rows.stream().mapToInt(EmployeeVoucherRepository.Row::quantity).sum();

        Map<Long, EmployeeAggregate> employees = new LinkedHashMap<>();
        for (EmployeeVoucherResponse voucher : vouchers.values()) {
            EmployeeAggregate aggregate = employees.computeIfAbsent(voucher.employeeId(), id -> new EmployeeAggregate(id, voucher.employeeName()));
            aggregate.count++; aggregate.total += voucher.totalCents(); aggregate.units += voucher.totalUnits();
        }
        Map<Long, EntryAggregate> entries = new LinkedHashMap<>();
        for (EmployeeVoucherRepository.Row row : rows) {
            EntryAggregate aggregate = entries.computeIfAbsent(row.catalogEntryId(), id -> new EntryAggregate(id, row.itemName(), row.type()));
            aggregate.quantity += row.quantity(); aggregate.total += row.lineTotalCents();
        }
        Map<LocalDate, DayAggregate> days = new TreeMap<>();
        for (EmployeeVoucherResponse voucher : vouchers.values()) {
            DayAggregate day = days.computeIfAbsent(voucher.closedAt().toLocalDate(), ignored -> new DayAggregate());
            day.count++; day.total += voucher.totalCents();
        }

        return new EmployeeVoucherReportResponse(
                new EmployeeVoucherReportResponse.Summary(total, vouchers.size(), vouchers.isEmpty() ? 0 : total / vouchers.size(), units),
                employees.values().stream().map(a -> new EmployeeVoucherReportResponse.ByEmployee(a.id, a.name, a.count, a.total, a.units)).toList(),
                entries.values().stream().map(a -> new EmployeeVoucherReportResponse.ByEntry(a.id, a.name, a.type, a.quantity, a.total, null)).toList(),
                days.entrySet().stream().map(e -> new EmployeeVoucherReportResponse.ByDay(e.getKey(), e.getValue().count, e.getValue().total)).toList()
        );
    }

    private List<EmployeeVoucherResponse> group(List<EmployeeVoucherRepository.Row> rows) {
        Map<Long, MutableVoucher> grouped = new LinkedHashMap<>();
        for (EmployeeVoucherRepository.Row row : rows) {
            MutableVoucher voucher = grouped.computeIfAbsent(row.operationId(), ignored -> new MutableVoucher(row));
            voucher.lines.add(new EmployeeVoucherResponse.Line(row.catalogEntryId(), row.itemName(), row.type(), row.quantity(), row.unitPriceCents(), row.lineTotalCents()));
        }
        return grouped.values().stream().map(MutableVoucher::response).toList();
    }

    private static final class MutableVoucher {
        final EmployeeVoucherRepository.Row row; final List<EmployeeVoucherResponse.Line> lines = new ArrayList<>();
        MutableVoucher(EmployeeVoucherRepository.Row row) { this.row = row; }
        EmployeeVoucherResponse response() { return new EmployeeVoucherResponse(row.operationId(), row.employeeId(), row.employeeName(),
                row.registeredTabName(), row.closedAt(), row.totalCents(), lines.size(), lines.stream().mapToInt(EmployeeVoucherResponse.Line::quantity).sum(),
                row.operatorUserId(), row.operatorName(), List.copyOf(lines)); }
    }
    private static final class EmployeeAggregate { final long id; final String name; int count; long total; int units; EmployeeAggregate(long id, String name) { this.id=id; this.name=name; } }
    private static final class EntryAggregate { final long id; final String name; final String type; int quantity; long total; EntryAggregate(long id,String name,String type){this.id=id;this.name=name;this.type=type;} }
    private static final class DayAggregate { int count; long total; }
}
