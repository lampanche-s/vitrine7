package br.com.vitrine7.report.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;

@Service
public class ReportPeriodAccessService {

    private final String protectedPassword;
    private final SalesReportFilterService filterService;
    private final Clock clock;

    public ReportPeriodAccessService(
            @Value("${REPORT_PROTECTED_PASSWORD:}") String protectedPassword,
            SalesReportFilterService filterService,
            Clock clock
    ) {
        this.protectedPassword = protectedPassword;
        this.filterService = filterService;
        this.clock = clock;
    }

    public boolean requiresPassword(LocalDate from, LocalDate to) {
        if (from == null || to == null || !from.equals(to)) {
            return true;
        }

        LocalDate today = LocalDate.now(clock.withZone(filterService.zoneId()));
        return !from.equals(today) && !from.equals(today.minusDays(1));
    }

    public boolean isPasswordValid(String suppliedPassword) {
        if (protectedPassword == null
                || protectedPassword.isEmpty()
                || suppliedPassword == null) {
            return false;
        }

        return MessageDigest.isEqual(
                protectedPassword.getBytes(StandardCharsets.UTF_8),
                suppliedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    public void requireAccess(
            LocalDate from,
            LocalDate to,
            String suppliedPassword
    ) {
        if (requiresPassword(from, to) && !isPasswordValid(suppliedPassword)) {
            throw new AccessDeniedException("Senha de acesso aos relatórios inválida.");
        }
    }
}
