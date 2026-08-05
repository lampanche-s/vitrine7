package br.com.vitrine7.auth.service;

import br.com.vitrine7.auth.dto.CurrentUserResponse;
import br.com.vitrine7.auth.dto.LoginRequest;
import br.com.vitrine7.common.exception.AccountBlockedException;
import br.com.vitrine7.common.exception.InvalidCredentialsException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.repository.UserRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import br.com.vitrine7.system.preference.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final JwtService jwtService;
    private final AuthSessionService authSessionService;

    @Transactional
    public LoginResult login(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username().trim(),
                            request.password()
                    )
            );

        } catch (DisabledException | LockedException exception) {
            throw new AccountBlockedException();

        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        VitrineUserPrincipal principal =
                (VitrineUserPrincipal) authentication.getPrincipal();

        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND",
                        "Usuário autenticado não foi encontrado."
                ));

        user.setLastLoginAt(OffsetDateTime.now());

        userPreferenceRepository.findById(user.getId())
                .ifPresent(preference ->
                        preference.setAdminModeEnabled(false)
                );

        JwtService.GeneratedToken token =
                jwtService.generate(principal);

        authSessionService.createSession(
                token.sessionId(),
                user.getId(),
                token.expiresAt()
        );


        return new LoginResult(
                token.value(),
                token.expiresAt(),
                CurrentUserResponse.from(principal)
        );
    }

    private String normalizeLoginIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value
                .trim()
                .toLowerCase(java.util.Locale.ROOT);

        return normalized.length() <= 120
                ? normalized
                : normalized.substring(0, 120);
    }

    public record LoginResult(
            String token,
            java.time.Instant expiresAt,
            CurrentUserResponse user
    ) {
    }
}
