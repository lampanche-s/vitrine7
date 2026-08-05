package br.com.vitrine7.system.settings.dto;

import br.com.vitrine7.system.settings.entity.SystemSettingsEntity;

import java.time.OffsetDateTime;

public record SystemSettingsResponse(
        String companyName,
        String cnpj,
        String phone,
        String address,
        boolean adminMode,
        OffsetDateTime updatedAt
) {

    public static SystemSettingsResponse from(
            SystemSettingsEntity settings,
            boolean adminMode
    ) {
        return new SystemSettingsResponse(
                settings.getCompanyName(),
                settings.getCnpj(),
                settings.getPhone(),
                settings.getAddress(),
                adminMode,
                settings.getUpdatedAt()
        );
    }
}
