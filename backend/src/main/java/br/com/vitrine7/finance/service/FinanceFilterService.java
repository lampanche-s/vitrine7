package br.com.vitrine7.finance.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.finance.config.FinanceProperties;
import br.com.vitrine7.finance.dto.FinanceFilters;
import br.com.vitrine7.finance.dto.FinanceModule;
import br.com.vitrine7.finance.dto.FinancePaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class FinanceFilterService {

    private static final long MAX_DAYS = 366;

    private final FinanceProperties properties;

    public FinanceFilters build(
            LocalDate from,
            LocalDate to,
            FinanceModule module,
            CheckoutOperationType operationType,
            FinancePaymentMethod paymentMethod,
            String search
    ) {
        LocalDate normalizedFrom = from == null
                ? LocalDate.now(zoneId())
                : from;
        LocalDate normalizedTo = to == null
                ? normalizedFrom
                : to;

        validateRange(normalizedFrom, normalizedTo);
        validateCompatibility(module, operationType);

        ZoneId zone = zoneId();

        return new FinanceFilters(
                normalizedFrom,
                normalizedTo,
                normalizedFrom.atStartOfDay(zone).toInstant(),
                normalizedTo.plusDays(1).atStartOfDay(zone).toInstant(),
                module,
                operationType,
                paymentMethod,
                normalizeSearch(search),
                zone.getId()
        );
    }

    public ZoneId zoneId() {
        String configured = properties.businessTimeZone();
        if (configured == null || configured.isBlank()) {
            return ZoneId.of("America/Bahia");
        }

        return ZoneId.of(configured);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException(
                    "INVALID_FINANCE_DATE_RANGE",
                    "A data inicial deve ser menor ou igual a data final."
            );
        }

        if (from.plusDays(MAX_DAYS - 1).isBefore(to)) {
            throw new InvalidRequestException(
                    "FINANCE_DATE_RANGE_TOO_LARGE",
                    "O periodo financeiro deve ter no maximo 366 dias."
            );
        }
    }

    private void validateCompatibility(
            FinanceModule module,
            CheckoutOperationType operationType
    ) {
        if (module == null || operationType == null) {
            return;
        }

        FinanceModule operationModule =
                operationType == CheckoutOperationType.LAVA_WORK_ORDER
                        ? FinanceModule.LAVA
                        : FinanceModule.BAR;

        if (module != operationModule) {
            throw new InvalidRequestException(
                    "INCOMPATIBLE_FINANCE_FILTERS",
                    "O modulo informado e incompativel com o tipo de operacao."
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
}
