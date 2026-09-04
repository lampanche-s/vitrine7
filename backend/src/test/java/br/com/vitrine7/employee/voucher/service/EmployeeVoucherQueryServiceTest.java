package br.com.vitrine7.employee.voucher.service;

import br.com.vitrine7.employee.voucher.repository.EmployeeVoucherRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeVoucherQueryServiceTest {
    private final EmployeeVoucherRepository repository = mock(EmployeeVoucherRepository.class);
    private final EmployeeVoucherQueryService service = new EmployeeVoucherQueryService(repository);

    @Test
    void aggregatesTwoVoucherTabsWithoutPayments() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-27T00:00:00-03:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-28T00:00:00-03:00");
        when(repository.reportRows(null, from, to)).thenReturn(List.of(
                row(7208, 77705, 3, OffsetDateTime.parse("2026-08-27T10:12:18-03:00")),
                row(7209, 1370, 1, OffsetDateTime.parse("2026-08-27T10:14:31-03:00"))
        ));

        var report = service.report(null, from, to);

        assertEquals(2, report.summary().voucherCount());
        assertEquals(79075, report.summary().voucherValueCents());
        assertEquals(39537, report.summary().averageVoucherCents());
        assertEquals(4, report.summary().consumedUnits());
        assertEquals(2, report.byDay().get(0).voucherCount());
    }

    private EmployeeVoucherRepository.Row row(long operationId, long totalCents, int quantity, OffsetDateTime closedAt) {
        return new EmployeeVoucherRepository.Row(
                operationId, 1, "Cibele", "Cibele", closedAt, totalCents,
                10, "Operador", 3, "Produto", "ITEM", quantity,
                totalCents / quantity, totalCents
        );
    }
}
