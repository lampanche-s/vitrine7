package br.com.vitrine7.system.preference.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.system.preference.dto.UpdateAdminModeRequest;
import br.com.vitrine7.system.preference.dto.UserPreferenceResponse;
import br.com.vitrine7.system.preference.entity.UserPreferenceEntity;
import br.com.vitrine7.system.preference.repository.UserPreferenceRepository;
import br.com.vitrine7.system.user.entity.UserRole;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;

    @Transactional(readOnly = true)
    public UserPreferenceResponse getCurrent(Long userId) {
        boolean adminMode = userPreferenceRepository
                .findById(userId)
                .map(UserPreferenceEntity::isAdminModeEnabled)
                .orElse(false);

        return new UserPreferenceResponse(adminMode);
    }

    @Transactional
    public UserPreferenceResponse updateAdminMode(
            UpdateAdminModeRequest request,
            VitrineUserPrincipal principal
    ) {
        if (request.enabled()
                && principal.getRole() != UserRole.SUPER_ADMIN
                && principal.getRole() != UserRole.ADMINISTRADOR) {
            throw new BusinessException(
                    "ADMIN_MODE_NOT_AVAILABLE",
                    "O modo administrativo está disponível apenas para administradores."
            );
        }

        UserPreferenceEntity preference = userPreferenceRepository
                .findById(principal.getId())
                .orElseGet(() -> UserPreferenceEntity.create(principal.getId()));

        preference.setAdminModeEnabled(request.enabled());

        UserPreferenceEntity saved = userPreferenceRepository.save(preference);

        return new UserPreferenceResponse(saved.isAdminModeEnabled());
    }
}
