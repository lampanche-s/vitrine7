package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.security.SecurityErrorWriter;
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
public class TerminalDeviceAuthenticationFilter extends OncePerRequestFilter {
    public static final String TOKEN_HEADER = "X-Terminal-Device-Token";
    private static final String PREFIX = "/api/v1/payment-terminal/bridge";
    private final TerminalDeviceService devices;
    private final SecurityErrorWriter errors;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(PREFIX) || path.equals(PREFIX + "/pair");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            TerminalDevicePrincipal principal = devices.authenticate(request.getHeader(TOKEN_HEADER));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("payment-terminal:bridge")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            int status = exception.getCode().equals("PAYMENT_TERMINAL_DEVICE_REVOKED")
                    ? HttpStatus.FORBIDDEN.value() : HttpStatus.UNAUTHORIZED.value();
            errors.write(request, response, status, exception.getCode(), exception.getMessage());
        }
    }
}
