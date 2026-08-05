package br.com.vitrine7.history.lava.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.history.bar.dto.HistoryPaymentMethod;
import br.com.vitrine7.history.lava.dto.LavaHistoryFilters;
import br.com.vitrine7.history.lava.dto.LavaHistoryResponse;
import br.com.vitrine7.history.lava.repository.LavaHistoryReadRepository;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LavaHistoryService {

    private final LavaHistoryReadRepository repository;

    public List<LavaHistoryResponse> recent(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);

        return repository.findRecent(normalizedLimit);
    }

    public PageResponse<LavaHistoryResponse> list(
            int page,
            int size,
            String search,
            LavaWorkOrderStatus status,
            OffsetDateTime from,
            OffsetDateTime to,
            HistoryPaymentMethod paymentMethod
    ) {
        validatePage(page, size);
        validateDates(from, to);

        LavaHistoryFilters filters = new LavaHistoryFilters(
                page,
                size,
                search,
                normalizeSearch(search),
                status,
                from,
                to,
                paymentMethod
        );

        List<LavaHistoryResponse> items =
                repository.findPage(filters);
        long totalElements = repository.count(filters);

        return pageResponse(items, page, size, totalElements);
    }

    private int normalizeLimit(Integer limit) {
        int value = limit == null ? 6 : limit;

        if (value < 1 || value > 20) {
            throw new InvalidRequestException(
                    "INVALID_HISTORY_LIMIT",
                    "O limite deve estar entre 1 e 20."
            );
        }

        return value;
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                    "INVALID_HISTORY_PAGE",
                    "A pagina deve ser maior ou igual a zero."
            );
        }

        if (size < 1 || size > 100) {
            throw new InvalidRequestException(
                    "INVALID_HISTORY_SIZE",
                    "O tamanho da pagina deve estar entre 1 e 100."
            );
        }
    }

    private void validateDates(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException(
                    "INVALID_HISTORY_DATE_RANGE",
                    "A data inicial deve ser menor ou igual a data final."
            );
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String withoutAccents = Normalizer
                .normalize(search, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private PageResponse<LavaHistoryResponse> pageResponse(
            List<LavaHistoryResponse> items,
            int page,
            int size,
            long totalElements
    ) {
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
}
