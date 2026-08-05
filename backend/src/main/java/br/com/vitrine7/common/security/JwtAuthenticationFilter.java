package br.com.vitrine7.common.security;

import br.com.vitrine7.auth.service.JwtService;
import br.com.vitrine7.auth.service.AuthSessionService;
import br.com.vitrine7.system.user.security.VitrineUserDetailsService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthSessionService authSessionService;
    private final VitrineUserDetailsService userDetailsService;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            findAuthenticationCookie(request)
                    .ifPresent(token -> authenticate(token, request));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(
            String token,
            HttpServletRequest request
    ) {
        try {
            Jwt jwt = jwtService.decode(token);

            VitrineUserPrincipal principal =
                    (VitrineUserPrincipal) userDetailsService
                            .loadUserByUsername(jwt.getSubject());

            if (!principal.isEnabled()
                    || !principal.isAccountNonLocked()
                    || !matchesAuthVersion(jwt, principal)
                    || !authSessionService.validateAndTouch(jwt, request)) {
                return;
            }

            authSessionService.exposeCurrentSession(
                    jwt,
                    request
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (JwtException | UsernameNotFoundException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean matchesAuthVersion(
            Jwt jwt,
            VitrineUserPrincipal principal
    ) {
        Object claim = jwt.getClaim("authVersion");

        if (!(claim instanceof Number number)) {
            return false;
        }

        return number.longValue() == principal.getAuthVersion();
    }

    private Optional<String> findAuthenticationCookie(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> securityProperties
                        .authCookieName()
                        .equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }
}
