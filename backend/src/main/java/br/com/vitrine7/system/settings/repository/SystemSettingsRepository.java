package br.com.vitrine7.system.settings.repository;

import br.com.vitrine7.system.settings.entity.SystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository
        extends JpaRepository<SystemSettingsEntity, Short> {
}
