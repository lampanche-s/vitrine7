package br.com.vitrine7.auth.controller;

import br.com.vitrine7.auth.dto.CsrfResponse;
import br.com.vitrine7.auth.dto.CurrentUserResponse;
import br.com.vitrine7.auth.dto.LoginRequest;
import br.com.vitrine7.auth.dto.LoginResponse;
import br.com.vitrine7.auth.service.AuthCookieService;
import br.com.vitrine7.auth.service.AuthService;
import br.com.vitrine7.auth.service.AuthSessionService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final AuthSessionService authSessionService;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginResult result =
                authService.login(request);

        authCookieService.addAuthenticationCookie(
                response,
                result.token()
        );

        return new LoginResponse(
                result.user(),
                result.expiresAt()
        );
    }

    @GetMapping("/me")
    public CurrentUserResponse me(
            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return CurrentUserResponse.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authSessionService.revokeCurrentSession(request);
        authCookieService.clearAuthenticationCookie(response);

        return ResponseEntity.noContent().build();
    }
}
