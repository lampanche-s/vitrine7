package br.com.vitrine7.system.user.security;

import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitrineUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionService rolePermissionService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado."
                ));

        return new VitrineUserPrincipal(
                user,
                rolePermissionService.getPermissions(user.getRole())
        );
    }
}
