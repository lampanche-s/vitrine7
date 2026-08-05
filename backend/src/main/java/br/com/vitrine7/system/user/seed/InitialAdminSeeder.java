package br.com.vitrine7.system.user.seed;

import br.com.vitrine7.common.security.SecurityProperties;
import br.com.vitrine7.common.security.PasswordPolicy;
import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import br.com.vitrine7.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminSeeder
        implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapSuperAdminIfConfigured();

        if (userRepository.count() == 0) {
            bootstrapInitialAdmin();
        }
    }

    private void bootstrapInitialAdmin() {
        SecurityProperties.InitialAdmin initialAdmin =
                securityProperties.initialAdmin();

        validate(initialAdmin);

        UserEntity administrator = UserEntity.create(
                initialAdmin.name().trim(),
                initialAdmin.username().trim(),
                null,
                passwordEncoder.encode(initialAdmin.password()),
                UserRole.ADMINISTRADOR,
                UserStatus.ATIVO
        );

        userRepository.save(administrator);

        log.info(
                "Usuário administrador inicial criado com username '{}'.",
                administrator.getUsername()
        );
    }

    private void bootstrapSuperAdminIfConfigured() {
        SecurityProperties.SuperAdmin superAdmin =
                securityProperties.superAdmin();

        if (superAdmin == null
                || !StringUtils.hasText(superAdmin.username())
                || !StringUtils.hasText(superAdmin.password())) {
            return;
        }

        validate(superAdmin);

        boolean exists =
                userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                        superAdmin.username().trim()
                );

        if (exists) {
            return;
        }

        UserEntity administrator = UserEntity.create(
                "SuperAdmin",
                superAdmin.username().trim(),
                null,
                passwordEncoder.encode(superAdmin.password()),
                UserRole.SUPER_ADMIN,
                UserStatus.ATIVO
        );

        userRepository.save(administrator);

        log.info(
                "Usuário SuperAdmin inicial criado por configuração segura."
        );
    }

    private void validate(
            SecurityProperties.InitialAdmin initialAdmin
    ) {
        if (initialAdmin == null) {
            throw new IllegalStateException(
                    "Configuração do administrador inicial ausente."
            );
        }

        if (!StringUtils.hasText(initialAdmin.name())) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_NAME é obrigatório."
            );
        }

        if (!StringUtils.hasText(initialAdmin.username())) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_USERNAME é obrigatório."
            );
        }

        if (!StringUtils.hasText(initialAdmin.password())
                || initialAdmin.password().length()
                        < PasswordPolicy.MIN_LENGTH) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD deve possuir pelo menos 6 caracteres."
            );
        }
    }

    private void validate(
            SecurityProperties.SuperAdmin superAdmin
    ) {
        if (!StringUtils.hasText(superAdmin.username())) {
            throw new IllegalStateException(
                    "VITRINE7_SUPERADMIN_USERNAME é obrigatório quando o bootstrap do SuperAdmin estiver habilitado."
            );
        }

        if (!StringUtils.hasText(superAdmin.password())
                || superAdmin.password().length()
                        < PasswordPolicy.MIN_LENGTH) {
            throw new IllegalStateException(
                    "VITRINE7_SUPERADMIN_PASSWORD deve possuir pelo menos 6 caracteres."
            );
        }
    }

}
