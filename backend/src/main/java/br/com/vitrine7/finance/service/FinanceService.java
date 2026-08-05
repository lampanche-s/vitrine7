package br.com.vitrine7.finance.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.finance.dto.FinanceDailyResponse;
import br.com.vitrine7.finance.dto.FinanceFilters;
import br.com.vitrine7.finance.dto.FinanceModule;
import br.com.vitrine7.finance.dto.FinancePaymentMethod;
import br.com.vitrine7.finance.dto.FinanceSummaryResponse;
import br.com.vitrine7.finance.dto.FinanceTransactionResponse;
import br.com.vitrine7.finance.repository.FinanceReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceFilterService filterService;
    private final FinanceReadRepository repository;

    public FinanceSummaryResponse summary(
            LocalDate from,
            LocalDate to,
            FinanceModule module,
            CheckoutOperationType operationType,
            FinancePaymentMethod paymentMethod
    ) {
        FinanceFilters filters = filterService.build(
                from,
                to,
                module,
                operationType,
                paymentMethod,
                null
        );

        return repository.summary(filters);
    }

    public List<FinanceDailyResponse> daily(
            LocalDate from,
            LocalDate to,
            FinanceModule module,
            CheckoutOperationType operationType,
            FinancePaymentMethod paymentMethod
    ) {
        FinanceFilters filters = filterService.build(
                from,
                to,
                module,
                operationType,
                paymentMethod,
                null
        );

        return repository.daily(filters);
    }

    public PageResponse<FinanceTransactionResponse> transactions(
            int page,
            int size,
            LocalDate from,
            LocalDate to,
            FinanceModule module,
            CheckoutOperationType operationType,
            FinancePaymentMethod paymentMethod,
            String search
    ) {
        validatePage(page, size);

        FinanceFilters filters = filterService.build(
                from,
                to,
                module,
                operationType,
                paymentMethod,
                search
        );

        List<FinanceTransactionResponse> items =
                repository.transactions(filters, page, size);
        long totalElements = repository.countTransactions(filters);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                    "INVALID_FINANCE_PAGE",
                    "A pagina deve ser maior ou igual a zero."
            );
        }

        if (size < 1 || size > 100) {
            throw new InvalidRequestException(
                    "INVALID_FINANCE_SIZE",
                    "O tamanho da pagina deve estar entre 1 e 100."
            );
        }
    }
}
