package br.com.vitrine7.employee.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Version @Column(nullable = false)
    private Long version;

    public static EmployeeEntity create(String name, String normalizedName) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.name = name;
        employee.normalizedName = normalizedName;
        return employee;
    }

    public void update(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public void softDelete(Long actorUserId, OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
        this.deletedByUserId = actorUserId;
    }
}
