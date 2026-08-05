package br.com.vitrine7.auth.service;

import br.com.vitrine7.common.security.SecurityProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private final SecurityProperties securityProperties;

    public void addAuthenticationCookie(
            HttpServletResponse response,
            String token
    ) {
        ResponseCookie cookie = ResponseCookie
                .from(securityProperties.authCookieName(), token)
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .sameSite(securityProperties.cookieSameSite())
                .path("/")
                .maxAge(securityProperties.jwtExpiration())
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public void clearAuthenticationCookie(
            HttpServletResponse response
    ) {
        ResponseCookie cookie = ResponseCookie
                .from(securityProperties.authCookieName(), "")
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .sameSite(securityProperties.cookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
}
