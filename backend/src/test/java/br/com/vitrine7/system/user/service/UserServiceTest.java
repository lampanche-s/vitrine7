package br.com.vitrine7.system.user.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.system.user.dto.CreateUserRequest;
import br.com.vitrine7.system.user.dto.ResetUserPasswordRequest;
import br.com.vitrine7.system.user.dto.UpdateUserRequest;
import br.com.vitrine7.system.user.dto.UserResponse;
import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import br.com.vitrine7.system.user.repository.UserRepository;
import br.com.vitrine7.system.user.security.Permission;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository =
            mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder =
            mock(PasswordEncoder.class);

    private final UserService userService =
            new UserService(
                    userRepository,
                    passwordEncoder
            );

    @Test
    void createTrimsUsernameAndDoesNotUseEmail() {
        when(passwordEncoder.encode("abc123"))
                .thenReturn("hash");
        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "Operador Caixa"
        )).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(8L);
                    return user;
                });

        UserResponse response = userService.create(
                new CreateUserRequest(
                        "Operador",
                        " Operador Caixa ",
                        "abc123",
                        UserRole.OPERADOR,
                        UserStatus.ATIVO
                ),
                principal(1L, UserRole.ADMINISTRADOR)
        );

        assertEquals("Operador Caixa", response.username());
        verify(userRepository).saveAndFlush(any(UserEntity.class));
    }

    @Test
    void createRejectsDuplicatedUsernameIgnoringCase() {
        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "OPERADOR"
        )).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.create(
                        new CreateUserRequest(
                                "Operador",
                                "OPERADOR",
                                "abc123",
                                UserRole.OPERADOR,
                                UserStatus.ATIVO
                        ),
                        principal(1L, UserRole.ADMINISTRADOR)
                )
        );

        assertEquals(
                "Este nome de usuário já está em uso.",
                exception.getMessage()
        );
    }

    @Test
    void administratorCannotCreateAnotherAdministrator() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> userService.create(
                        new CreateUserRequest(
                                "Administrador",
                                "admin2",
                                "abc123",
                                UserRole.ADMINISTRADOR,
                                UserStatus.ATIVO
                        ),
                        principal(1L, UserRole.ADMINISTRADOR)
                )
        );
    }

    @Test
    void superAdminCanCreateAdministrator() {
        when(passwordEncoder.encode("abc123"))
                .thenReturn("hash");
        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "admin2"
        )).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(8L);
                    return user;
                });

        UserResponse response = userService.create(
                new CreateUserRequest(
                        "Administrador",
                        "admin2",
                        "abc123",
                        UserRole.ADMINISTRADOR,
                        UserStatus.ATIVO
                ),
                principal(99L, UserRole.SUPER_ADMIN)
        );

        assertEquals("ADMINISTRADOR", response.role());
    }

    @Test
    void updateUsernameTrimsAndBumpsAuthVersion() {
        UserEntity user = UserEntity.create(
                "Operador",
                "operador",
                "legacy@example.com",
                "hash",
                UserRole.OPERADOR,
                UserStatus.ATIVO
        );
        user.setId(9L);

        when(userRepository.findByIdAndDeletedAtIsNull(9L))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNotAndDeletedAtIsNull(
                "Operador.Novo @ Loja",
                9L
        )).thenReturn(false);

        UserResponse response = userService.update(
                9L,
                new UpdateUserRequest(
                        "Operador Novo",
                        " Operador.Novo @ Loja ",
                        UserRole.OPERADOR
                ),
                principal(1L, UserRole.ADMINISTRADOR)
        );

        assertEquals("Operador.Novo @ Loja", response.username());
        assertEquals(1L, user.getAuthVersion());
        assertEquals("legacy@example.com", user.getEmail());
        verify(userRepository).flush();
    }

    @Test
    void resetPasswordWithSixCharactersUpdatesHashAndAuthVersion() {
        UserEntity user = UserEntity.create(
                "Operador",
                "operador",
                null,
                "old-hash",
                UserRole.OPERADOR,
                UserStatus.ATIVO
        );
        user.setId(9L);

        when(userRepository.findByIdAndDeletedAtIsNull(9L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("abc123", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("abc123"))
                .thenReturn("new-hash");

        userService.resetPassword(
                9L,
                new ResetUserPasswordRequest("abc123"),
                principal(1L, UserRole.ADMINISTRADOR)
        );

        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(1L, user.getAuthVersion());
    }

    private VitrineUserPrincipal principal(
            Long id,
            UserRole role
    ) {
        UserEntity user = UserEntity.create(
                role == UserRole.SUPER_ADMIN
                        ? "SuperAdmin"
                        : "Administrador",
                "actor" + id,
                null,
                "hash",
                role,
                UserStatus.ATIVO
        );
        user.setId(id);

        return new VitrineUserPrincipal(
                user,
                Set.of(Permission.ADMIN_USERS)
        );
    }
}
