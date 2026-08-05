package br.com.vitrine7.system.settings.service;

import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.system.preference.repository.UserPreferenceRepository;
import br.com.vitrine7.system.settings.dto.SystemSettingsResponse;
import br.com.vitrine7.system.settings.dto.UpdateSystemSettingsRequest;
import br.com.vitrine7.system.settings.entity.SystemSettingsEntity;
import br.com.vitrine7.system.settings.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SystemSettingsRepository systemSettingsRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Transactional(readOnly = true)
    public SystemSettingsResponse getCurrent(Long userId) {
        return buildResponse(
                getSettings(),
                getAdminMode(userId)
        );
    }

    @Transactional
    public SystemSettingsResponse update(
            UpdateSystemSettingsRequest request,
            Long userId
    ) {
        SystemSettingsEntity settings = getSettings();

        settings.update(
                request.companyName(),
                request.cnpj(),
                request.phone(),
                request.address()
        );

        return buildResponse(
                settings,
                getAdminMode(userId)
        );
    }

    private SystemSettingsEntity getSettings() {
        return systemSettingsRepository
                .findById(SystemSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new NotFoundException(
                        "SYSTEM_SETTINGS_NOT_FOUND",
                        "As configurações do sistema não foram encontradas."
                ));
    }

    private boolean getAdminMode(Long userId) {
        return userPreferenceRepository
                .findById(userId)
                .map(preference -> preference.isAdminModeEnabled())
                .orElse(false);
    }

    private SystemSettingsResponse buildResponse(
            SystemSettingsEntity settings,
            boolean adminMode
    ) {
        return SystemSettingsResponse.from(
                settings,
                adminMode
        );
    }
}
