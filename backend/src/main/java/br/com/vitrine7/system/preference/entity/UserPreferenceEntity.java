package br.com.vitrine7.system.preference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "admin_mode_enabled", nullable = false)
    private boolean adminModeEnabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static UserPreferenceEntity create(Long userId) {
        UserPreferenceEntity preference = new UserPreferenceEntity();

        preference.userId = userId;
        preference.adminModeEnabled = false;

        return preference;
    }
}
