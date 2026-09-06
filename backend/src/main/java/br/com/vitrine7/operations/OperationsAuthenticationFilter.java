package br.com.vitrine7.operations;

import br.com.vitrine7.common.security.SecurityErrorWriter;
import br.com.vitrine7.report.service.ReportPeriodAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OperationsAuthenticationFilter extends OncePerRequestFilter {
    public static final String CREDENTIAL_HEADER = "X-Report-Password";
    private static final String PREFIX = "/api/v1/operations/";

    private final ReportPeriodAccessService credentials;
    private final SecurityErrorWriter errors;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX)
                || request.getHeader(CREDENTIAL_HEADER) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        if (!credentials.isPasswordValid(
                request.getHeader(CREDENTIAL_HEADER))) {
            SecurityContextHolder.clearContext();
            errors.write(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    "OPERATIONS_UNAUTHORIZED",
                    "Credencial operacional invalida."
            );
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "vitrine7-operations",
                null,
                List.of(new SimpleGrantedAuthority(
                        "operations:agents-status"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
