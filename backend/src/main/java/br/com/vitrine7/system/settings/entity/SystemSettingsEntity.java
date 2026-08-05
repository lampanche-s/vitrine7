package br.com.vitrine7.system.settings.entity;

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
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemSettingsEntity {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    @Column(nullable = false, length = 18)
    private String cnpj;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public void update(
            String companyName,
            String cnpj,
            String phone,
            String address
    ) {
        this.companyName = companyName.trim();
        this.cnpj = normalizeOptional(cnpj);
        this.phone = normalizeOptional(phone);
        this.address = normalizeOptional(address);
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
