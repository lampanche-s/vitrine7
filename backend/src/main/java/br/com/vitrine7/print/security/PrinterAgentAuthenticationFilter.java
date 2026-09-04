package br.com.vitrine7.print.security;

import br.com.vitrine7.common.security.SecurityErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class PrinterAgentAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Printer-Agent-Token";
    private static final String PREFIX = "/api/v1/printer-agent";

    private final PrinterAgentProperties properties;
    private final SecurityErrorWriter errors;

    public PrinterAgentAuthenticationFilter(
            PrinterAgentProperties properties,
            SecurityErrorWriter errors
    ) {
        this.properties = properties;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.equals(PREFIX) || uri.startsWith(PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String configuredToken = properties.token();
        String suppliedToken = request.getHeader(TOKEN_HEADER);

        if (!matches(configuredToken, suppliedToken)) {
            SecurityContextHolder.clearContext();
            errors.write(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    "PRINTER_AGENT_UNAUTHORIZED",
                    "Agente de impressao nao autorizado."
            );
            return;
        }

        PrinterAgentPrincipal principal =
                new PrinterAgentPrincipal("vitrine7-printer-agent");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("printer-agent:bridge"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean matches(String configuredToken, String suppliedToken) {
        if (configuredToken == null || configuredToken.isBlank()
                || suppliedToken == null || suppliedToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
