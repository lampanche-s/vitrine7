package br.com.vitrine7.system.user.dto;

import br.com.vitrine7.system.user.entity.UserEntity;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String name,
        String username,
        String role,
        String status,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
