package br.com.vitrine7.system.preference.repository;

import br.com.vitrine7.system.preference.entity.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository
        extends JpaRepository<UserPreferenceEntity, Long> {
}
