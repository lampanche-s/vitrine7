package br.com.vitrine7.system.settings.controller;

import br.com.vitrine7.system.settings.dto.SystemSettingsResponse;
import br.com.vitrine7.system.settings.dto.UpdateSystemSettingsRequest;
import br.com.vitrine7.system.settings.service.SettingsService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public SystemSettingsResponse getCurrent(
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return settingsService.getCurrent(principal.getId());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('admin:settings')")
    public SystemSettingsResponse update(
            @Valid @RequestBody UpdateSystemSettingsRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return settingsService.update(request, principal.getId());
    }
}
