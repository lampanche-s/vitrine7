package br.com.vitrine7.system.user.seed;

import br.com.vitrine7.common.security.SecurityProperties;
import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialAdminSeederTest {

    private final UserRepository userRepository =
            mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder =
            mock(PasswordEncoder.class);
    private final ApplicationArguments arguments =
            mock(ApplicationArguments.class);

    @Test
    void createsConfiguredSuperAdminWhenMissing() {
        InitialAdminSeeder seeder =
                new InitialAdminSeeder(
                        userRepository,
                        passwordEncoder,
                        securityProperties(
                                "root",
                                "Vitrine7@Super123"
                        )
                );

        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "root"
        )).thenReturn(false);
        when(passwordEncoder.encode("Vitrine7@Super123"))
                .thenReturn("hash");
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run(arguments);

        verify(userRepository).save(any(UserEntity.class));
        verify(passwordEncoder).encode("Vitrine7@Super123");
    }

    @Test
    void doesNotDuplicateConfiguredSuperAdminOnRestart() {
        InitialAdminSeeder seeder =
                new InitialAdminSeeder(
                        userRepository,
                        passwordEncoder,
                        securityProperties(
                                "root",
                                "Vitrine7@Super123"
                        )
                );

        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "root"
        )).thenReturn(true);
        when(userRepository.count()).thenReturn(1L);

        seeder.run(arguments);

        verify(userRepository, never()).save(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void storesSuperAdminWithDedicatedRole() {
        InitialAdminSeeder seeder =
                new InitialAdminSeeder(
                        userRepository,
                        passwordEncoder,
                        securityProperties(
                                "root",
                                "Vitrine7@Super123"
                        )
                );

        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "root"
        )).thenReturn(false);
        when(passwordEncoder.encode("Vitrine7@Super123"))
                .thenReturn("hash");
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run(arguments);

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                user -> {
                    assertEquals(UserRole.SUPER_ADMIN, user.getRole());
                    assertEquals("hash", user.getPasswordHash());
                    return true;
                }
        ));
    }

    @Test
    void acceptsConfiguredSuperAdminPasswordWithExactlySixCharacters() {
        InitialAdminSeeder seeder =
                new InitialAdminSeeder(
                        userRepository,
                        passwordEncoder,
                        securityProperties(
                                "root",
                                "123456"
                        )
                );

        when(userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                "root"
        )).thenReturn(false);
        when(passwordEncoder.encode("123456"))
                .thenReturn("hash");
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run(arguments);

        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void rejectsConfiguredSuperAdminPasswordWithLessThanSixCharacters() {
        InitialAdminSeeder seeder =
                new InitialAdminSeeder(
                        userRepository,
                        passwordEncoder,
                        securityProperties(
                                "root",
                                "12345"
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () -> seeder.run(arguments)
        );
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(any());
    }

    private SecurityProperties securityProperties(
            String superAdminUsername,
            String superAdminPassword
    ) {
        return new SecurityProperties(
                "issuer",
                "secret",
                Duration.ofHours(8),
                Duration.ofHours(5),
                Duration.ofMinutes(5),
                "AUTH",
                false,
                "Lax",
                "http://localhost:5173",
                new SecurityProperties.InitialAdmin(
                        "Admin",
                        "admin",
                        "Vitrine7@Admin123"
                ),
                new SecurityProperties.SuperAdmin(
                        superAdminUsername,
                        superAdminPassword
                )
        );
    }
}
