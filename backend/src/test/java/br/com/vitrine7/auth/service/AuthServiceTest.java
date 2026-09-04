package br.com.vitrine7.auth.service;

import br.com.vitrine7.auth.dto.LoginRequest;
import br.com.vitrine7.common.exception.AccountBlockedException;
import br.com.vitrine7.common.exception.InvalidCredentialsException;
import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import br.com.vitrine7.system.user.repository.UserRepository;
import br.com.vitrine7.system.user.security.Permission;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final LoginRequest LOGIN_REQUEST =
            new LoginRequest(
                    "operador",
                    "senha-segura"
            );

    private final AuthenticationManager authenticationManager =
            mock(AuthenticationManager.class);
    private final UserRepository userRepository =
            mock(UserRepository.class);
    private final JwtService jwtService =
            mock(JwtService.class);
    private final AuthSessionService authSessionService =
            mock(AuthSessionService.class);

    private final AuthService authService =
            new AuthService(
                    authenticationManager,
                    userRepository,
                    jwtService,
                    authSessionService
            );

    @Test
    void activeUserWithCorrectPasswordReceivesTokenAndSession() {
        UserEntity user = user(UserStatus.ATIVO, false);
        VitrineUserPrincipal principal =
                new VitrineUserPrincipal(user, Set.of(Permission.BAR_ACCESS));
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        JwtService.GeneratedToken token =
                new JwtService.GeneratedToken(
                        "jwt-token",
                        Instant.parse("2026-07-23T12:00:00Z"),
                        UUID.randomUUID()
                );

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(jwtService.generate(principal))
                .thenReturn(token);

        AuthService.LoginResult result =
                authService.login(LOGIN_REQUEST);

        assertEquals("jwt-token", result.token());
        assertEquals(user.getId(), result.user().id());
        verify(authSessionService).createSession(
                token.sessionId(),
                user.getId(),
                token.expiresAt()
        );
    }

    @Test
    void blockedUserReceivesStableBlockedErrorAndNoSession() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("bloqueado"));

        AccountBlockedException exception =
                assertThrows(
                        AccountBlockedException.class,
                        () -> authService.login(LOGIN_REQUEST)
                );

        assertEquals(
                AccountBlockedException.CODE,
                "ACCOUNT_BLOCKED"
        );
        assertEquals(
                "Este usuário está bloqueado.",
                exception.getMessage()
        );
        verify(jwtService, never()).generate(any());
        verify(authSessionService, never())
                .createSession(any(), any(), any());
    }

    @Test
    void invalidPasswordKeepsInvalidCredentialsRule() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("senha invalida"));

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.login(LOGIN_REQUEST)
                );

        assertEquals(
                "Usuário ou senha inválidos.",
                exception.getMessage()
        );
        verify(jwtService, never()).generate(any());
        verify(authSessionService, never())
                .createSession(any(), any(), any());
    }

    @Test
    void deletedUserIsTreatedAsInvalidCredentialsByLoginLookup() {
        UserEntity deletedUser = user(UserStatus.BLOQUEADO, true);

        assertEquals(UserStatus.BLOQUEADO, deletedUser.getStatus());
        assertEquals(true, deletedUser.isDeleted());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("usuario excluido"));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(LOGIN_REQUEST)
        );
        verify(jwtService, never()).generate(any());
        verify(authSessionService, never())
                .createSession(any(), any(), any());
    }

    @Test
    void unblockedUserCanLoginAgain() {
        UserEntity user = user(UserStatus.ATIVO, false);
        VitrineUserPrincipal principal =
                new VitrineUserPrincipal(user, Set.of());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        JwtService.GeneratedToken token =
                new JwtService.GeneratedToken(
                        "jwt-token",
                        Instant.parse("2026-07-23T12:00:00Z"),
                        UUID.randomUUID()
                );

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(jwtService.generate(principal))
                .thenReturn(token);

        AuthService.LoginResult result =
                authService.login(LOGIN_REQUEST);

        assertEquals("jwt-token", result.token());
    }

    @Test
    void administratorCanLoginWithUsernameAndPassword() {
        AuthService.LoginResult result =
                loginAs(UserRole.ADMINISTRADOR);

        assertEquals("ADMINISTRADOR", result.user().role());
    }

    @Test
    void superAdminCanLoginWithUsernameAndPassword() {
        AuthService.LoginResult result =
                loginAs(UserRole.SUPER_ADMIN);

        assertEquals("SUPER_ADMIN", result.user().role());
    }

    private AuthService.LoginResult loginAs(UserRole role) {
        UserEntity user = UserEntity.create(
                role.name(),
                role == UserRole.SUPER_ADMIN
                        ? "Root"
                        : "Admin",
                null,
                "hash",
                role,
                UserStatus.ATIVO
        );
        user.setId(role == UserRole.SUPER_ADMIN ? 99L : 8L);

        VitrineUserPrincipal principal =
                new VitrineUserPrincipal(user, Set.of());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        JwtService.GeneratedToken token =
                new JwtService.GeneratedToken(
                        "jwt-token",
                        Instant.parse("2026-07-23T12:00:00Z"),
                        UUID.randomUUID()
                );

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(jwtService.generate(principal))
                .thenReturn(token);

        return authService.login(new LoginRequest(
                user.getUsername(),
                "abc123"
        ));
    }

    private UserEntity user(
            UserStatus status,
            boolean deleted
    ) {
        UserEntity user = UserEntity.create(
                "Operador",
                "operador",
                "operador@vitrine7.com",
                "hash",
                UserRole.OPERADOR,
                status
        );

        user.setId(7L);

        if (deleted) {
            user.softDelete(1L);
        }

        return user;
    }
}
