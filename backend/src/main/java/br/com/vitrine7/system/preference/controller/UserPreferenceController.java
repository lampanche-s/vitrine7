package br.com.vitrine7.system.preference.controller;

import br.com.vitrine7.system.preference.dto.UpdateAdminModeRequest;
import br.com.vitrine7.system.preference.dto.UserPreferenceResponse;
import br.com.vitrine7.system.preference.service.UserPreferenceService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping("/me")
    public UserPreferenceResponse getCurrent(
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userPreferenceService.getCurrent(principal.getId());
    }

    @PatchMapping("/admin-mode")
    public UserPreferenceResponse updateAdminMode(
            @Valid @RequestBody UpdateAdminModeRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return userPreferenceService.updateAdminMode(request, principal);
    }
}
