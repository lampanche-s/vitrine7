package br.com.vitrine7.system.user.service;

import br.com.vitrine7.auth.service.AuthSessionService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.system.user.dto.ChangeUserStatusRequest;
import br.com.vitrine7.system.user.dto.CreateUserRequest;
import br.com.vitrine7.system.user.dto.ResetUserPasswordRequest;
import br.com.vitrine7.system.user.dto.UpdateUserRequest;
import br.com.vitrine7.system.user.dto.UserResponse;
import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import br.com.vitrine7.system.user.repository.UserRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import br.com.vitrine7.system.user.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "username",
            "role",
            "status",
            "createdAt",
            "updatedAt",
            "lastLoginAt"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(
            int page,
            int size,
            String sort,
            String direction,
            String search,
            UserStatus status,
            UserRole role,
            VitrineUserPrincipal actor
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(parseDirection(direction), parseSortField(sort))
        );

        Specification<UserEntity> specification = Specification.allOf(
                UserSpecifications.notDeleted(),
                UserSpecifications.matchesSearch(search),
                UserSpecifications.hasStatus(status),
                UserSpecifications.hasRole(role),
                visibilityScope(actor.getRole(), actor.getId())
        );

        Page<UserEntity> users = userRepository.findAll(
                specification,
                pageRequest
        );

        return PageResponse.from(users, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(
            Long id,
            VitrineUserPrincipal actor
    ) {
        return UserResponse.from(getVisibleUser(id, actor));
    }

    @Transactional
    public UserResponse create(
            CreateUserRequest request,
            VitrineUserPrincipal actor
    ) {
        validateRequestedRole(request.role(), actor);

        String username = normalizeUsername(request.username());

        validateUniqueUsername(username, null);

        UserEntity user = UserEntity.create(
                request.name(),
                username,
                null,
                passwordEncoder.encode(request.password()),
                request.role(),
                UserStatus.ATIVO
        );

        UserResponse response;

        try {
            response = UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw usernameAlreadyExists();
        }


        return response;
    }

    @Transactional
    public UserResponse update(
            Long id,
            UpdateUserRequest request,
            VitrineUserPrincipal actor
    ) {
        UserEntity user = getManageableUser(id, actor);

        String username = normalizeUsername(request.username());

        validateRequestedRole(request.role(), actor);

        validateSelfSecurityChange(
                user,
                actor.getId(),
                username,
                request.role()
        );

        validateLastAdministratorRoleChange(
                user,
                request.role(),
                actor.getRole()
        );
        validateUniqueUsername(username, id);

        boolean authenticationChanged =
                !user.getUsername().equals(username)
                        || user.getRole() != request.role();

        user.setName(request.name());
        user.setUsername(username);
        user.setRole(request.role());

        if (authenticationChanged) {
            user.incrementAuthVersion();
            authSessionService.revokeAllForUser(user.getId());
        }

        UserResponse response;

        try {
            userRepository.flush();
            response = UserResponse.from(user);
        } catch (DataIntegrityViolationException exception) {
            throw usernameAlreadyExists();
        }


        return response;
    }

    @Transactional
    public UserResponse changeStatus(
            Long id,
            ChangeUserStatusRequest request,
            VitrineUserPrincipal actor
    ) {
        UserEntity user = getManageableUser(id, actor);

        if (id.equals(actor.getId())
                && request.status() == UserStatus.BLOQUEADO) {
            throw new BusinessException(
                    "CANNOT_BLOCK_SELF",
                    "Você não pode bloquear o próprio usuário."
            );
        }

        if (user.getStatus() == request.status()) {
            return UserResponse.from(user);
        }

        validateLastAdministratorStatusChange(
                user,
                request.status(),
                actor.getRole()
        );

        user.setStatus(request.status());
        user.incrementAuthVersion();
        authSessionService.revokeAllForUser(user.getId());

        UserResponse response = UserResponse.from(user);


        return response;
    }

    @Transactional
    public void resetPassword(
            Long id,
            ResetUserPasswordRequest request,
            VitrineUserPrincipal actor
    ) {
        UserEntity user = getManageableUser(id, actor);

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new BusinessException(
                    "PASSWORD_NOT_CHANGED",
                    "A nova senha deve ser diferente da senha atual."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );
        user.incrementAuthVersion();
        authSessionService.revokeAllForUser(user.getId());

    }

    @Transactional
    public void delete(
            Long id,
            VitrineUserPrincipal actor
    ) {
        UserEntity user = getManageableUser(id, actor);

        if (id.equals(actor.getId())) {
            throw new BusinessException(
                    "CANNOT_DELETE_SELF",
                    "Você não pode excluir o próprio usuário."
            );
        }

        validateLastAdministratorDeletion(user, actor.getRole());
        user.softDelete(actor.getId());
        authSessionService.revokeAllForUser(user.getId());

    }

    private UserEntity getExistingUser(Long id) {
        return userRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND",
                        "Usuário não encontrado."
                ));
    }

    private UserEntity getVisibleUser(
            Long id,
            VitrineUserPrincipal actor
    ) {
        UserEntity user = getExistingUser(id);

        if (!isVisibleTo(user, actor)) {
            throwUserNotFound();
        }

        return user;
    }

    private UserEntity getManageableUser(
            Long id,
            VitrineUserPrincipal actor
    ) {
        UserEntity user = getVisibleUser(id, actor);

        if (!canManageRole(actor.getRole(), user.getRole())) {
            throw new AuthorizationDeniedException(
                    "Acesso negado."
            );
        }

        return user;
    }

    private Specification<UserEntity> visibilityScope(
            UserRole actorRole,
            Long actorUserId
    ) {
        if (actorRole == UserRole.SUPER_ADMIN) {
            return Specification.allOf(
                    UserSpecifications.notRole(UserRole.SUPER_ADMIN),
                    UserSpecifications.notId(actorUserId)
            );
        }

        if (actorRole == UserRole.ADMINISTRADOR) {
            return UserSpecifications.hasRole(UserRole.OPERADOR);
        }

        return (root, query, builder) -> builder.disjunction();
    }

    private boolean isVisibleTo(
            UserEntity user,
            VitrineUserPrincipal actor
    ) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return false;
        }

        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            return !user.getId().equals(actor.getId());
        }

        return actor.getRole() == UserRole.ADMINISTRADOR
                && user.getRole() == UserRole.OPERADOR;
    }

    private boolean canManageRole(
            UserRole actorRole,
            UserRole targetRole
    ) {
        if (targetRole == UserRole.SUPER_ADMIN) {
            return false;
        }

        return actorRole == UserRole.SUPER_ADMIN
                || actorRole == UserRole.ADMINISTRADOR
                        && targetRole == UserRole.OPERADOR;
    }

    private void validateRequestedRole(
            UserRole requestedRole,
            VitrineUserPrincipal actor
    ) {
        if (requestedRole == UserRole.SUPER_ADMIN) {
            throw new InvalidRequestException(
                    "INVALID_USER_ROLE",
                    "O perfil informado não é permitido."
            );
        }

        if (requestedRole == UserRole.ADMINISTRADOR
                && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new AuthorizationDeniedException(
                    "Acesso negado."
            );
        }
    }

    private void throwUserNotFound() {
        throw new NotFoundException(
                "USER_NOT_FOUND",
                "Usuário não encontrado."
        );
    }

    private void validateSelfSecurityChange(
            UserEntity user,
            Long actorUserId,
            String requestedUsername,
            UserRole requestedRole
    ) {
        if (!user.getId().equals(actorUserId)) {
            return;
        }

        boolean usernameChanged =
                !user.getUsername().equals(requestedUsername);
        boolean roleChanged = user.getRole() != requestedRole;

        if (usernameChanged || roleChanged) {
            throw new BusinessException(
                    "CANNOT_CHANGE_OWN_SECURITY_FIELDS",
                    "Você não pode alterar seu próprio usuário ou perfil por esta operação administrativa."
            );
        }
    }

    private void validateLastAdministratorRoleChange(
            UserEntity user,
            UserRole requestedRole,
            UserRole actorRole
    ) {
        if (actorRole == UserRole.SUPER_ADMIN) {
            return;
        }

        boolean removesActiveAdministrator =
                user.getRole() == UserRole.ADMINISTRADOR
                        && user.getStatus() == UserStatus.ATIVO
                        && requestedRole != UserRole.ADMINISTRADOR;

        if (removesActiveAdministrator) {
            ensureAnotherActiveAdministrator();
        }
    }

    private void validateLastAdministratorStatusChange(
            UserEntity user,
            UserStatus requestedStatus,
            UserRole actorRole
    ) {
        if (actorRole == UserRole.SUPER_ADMIN) {
            return;
        }

        boolean blocksActiveAdministrator =
                user.getRole() == UserRole.ADMINISTRADOR
                        && user.getStatus() == UserStatus.ATIVO
                        && requestedStatus == UserStatus.BLOQUEADO;

        if (blocksActiveAdministrator) {
            ensureAnotherActiveAdministrator();
        }
    }

    private void validateLastAdministratorDeletion(
            UserEntity user,
            UserRole actorRole
    ) {
        if (actorRole == UserRole.SUPER_ADMIN) {
            return;
        }

        boolean deletesActiveAdministrator =
                user.getRole() == UserRole.ADMINISTRADOR
                        && user.getStatus() == UserStatus.ATIVO;

        if (deletesActiveAdministrator) {
            ensureAnotherActiveAdministrator();
        }
    }

    private void ensureAnotherActiveAdministrator() {
        long activeAdministrators =
                userRepository.countByRoleAndStatusAndDeletedAtIsNull(
                        UserRole.ADMINISTRADOR,
                        UserStatus.ATIVO
                );

        if (activeAdministrators <= 1) {
            throw new BusinessException(
                    "LAST_ACTIVE_ADMINISTRATOR",
                    "A operação deixaria o sistema sem administrador ativo."
            );
        }
    }

    private void validateUniqueUsername(
            String username,
            Long ignoredUserId
    ) {
        boolean exists = ignoredUserId == null
                ? userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(
                        username
                )
                : userRepository.existsByUsernameIgnoreCaseAndIdNotAndDeletedAtIsNull(
                        username,
                        ignoredUserId
                );

        if (exists) {
            throw usernameAlreadyExists();
        }
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private BusinessException usernameAlreadyExists() {
        return new BusinessException(
                "USERNAME_ALREADY_EXISTS",
                "Este nome de usuário já está em uso."
        );
    }

    private Sort.Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.ASC;
        }

        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "INVALID_SORT_DIRECTION",
                    "A direção deve ser ASC ou DESC."
            );
        }
    }

    private String parseSortField(String sort) {
        String requested = sort == null || sort.isBlank()
                ? "name"
                : sort.trim();

        if (!ALLOWED_SORT_FIELDS.contains(requested)) {
            throw new InvalidRequestException(
                    "INVALID_SORT_FIELD",
                    "O campo de ordenação informado não é permitido."
            );
        }

        return requested;
    }
}
