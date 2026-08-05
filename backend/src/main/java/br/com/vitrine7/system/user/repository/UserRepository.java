package br.com.vitrine7.system.user.repository;

import br.com.vitrine7.system.user.entity.UserEntity;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends
        JpaRepository<UserEntity, Long>,
        JpaSpecificationExecutor<UserEntity> {

    @Query("""
            SELECT user
            FROM UserEntity user
            WHERE user.deletedAt IS NULL
              AND LOWER(user.username) = LOWER(:login)
            """)
    Optional<UserEntity> findByLogin(@Param("login") String login);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<UserEntity> findByUsernameIgnoreCaseAndDeletedAtIsNull(
            String username
    );

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(
            String username
    );

    boolean existsByUsernameIgnoreCaseAndIdNotAndDeletedAtIsNull(
            String username,
            Long id
    );

    long countByRoleAndStatusAndDeletedAtIsNull(
            UserRole role,
            UserStatus status
    );
}
