package br.com.vitrine7.system.user.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.system.user.dto.ChangeUserStatusRequest;
import br.com.vitrine7.system.user.dto.CreateUserRequest;
import br.com.vitrine7.system.user.dto.ResetUserPasswordRequest;
import br.com.vitrine7.system.user.dto.UpdateUserRequest;
import br.com.vitrine7.system.user.dto.UserResponse;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import br.com.vitrine7.system.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('admin:users')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página não pode ser negativa.")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho mínimo é 1.")
            @Max(value = 100, message = "O tamanho máximo é 100.")
            int size,

            @RequestParam(defaultValue = "name")
            String sort,

            @RequestParam(defaultValue = "ASC")
            String direction,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            UserStatus status,

            @RequestParam(required = false)
            UserRole role,

            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userService.list(
                page,
                size,
                sort,
                direction,
                search,
                status,
                role,
                principal
        );
    }

    @GetMapping("/{id}")
    public UserResponse findById(
            @PathVariable Long id,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userService.findById(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userService.create(request, principal);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userService.update(id, request, principal);
    }

    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserStatusRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userService.changeStatus(id, request, principal);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetUserPasswordRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        userService.resetPassword(id, request, principal);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        userService.delete(id, principal);

        return ResponseEntity.noContent().build();
    }
}
