package br.com.vitrine7.system.user.security;

import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class VitrineUserPrincipal implements UserDetails {

    private final Long id;
    private final String name;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final UserStatus status;
    private final Long authVersion;
    private final Set<Permission> permissions;

    public VitrineUserPrincipal(
            UserEntity user,
            Set<Permission> permissions
    ) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.authVersion = user.getAuthVersion();
        this.permissions = Set.copyOf(permissions);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public List<String> getPermissionCodes() {
        return permissions.stream()
                .map(Permission::getCode)
                .sorted()
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );

        permissions.stream()
                .map(Permission::getCode)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return List.copyOf(authorities);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BLOQUEADO;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ATIVO;
    }
}
