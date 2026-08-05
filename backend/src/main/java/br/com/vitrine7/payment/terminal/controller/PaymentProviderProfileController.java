package br.com.vitrine7.payment.terminal.controller;

import br.com.vitrine7.payment.terminal.dto.PaymentProviderAdminProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderCatalogResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderProfileRequest;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderPublicProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderStatusResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderTestResponse;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfileService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-terminal")
@PreAuthorize("hasAuthority('admin:payment-config')")
@RequiredArgsConstructor
public class PaymentProviderProfileController {

    private final PaymentProviderProfileService service;

    @GetMapping("/providers")
    public List<PaymentProviderCatalogResponse> listProviders() {
        return service.listCatalog();
    }

    @GetMapping("/providers/{providerCode}/profile")
    public PaymentProviderAdminProfileResponse getProviderProfile(
            @PathVariable String providerCode
    ) {
        return service.getProviderProfile(providerCode);
    }

    @PostMapping("/providers/{providerCode}/activate")
    public PaymentProviderPublicProfileResponse activateProvider(
            @PathVariable String providerCode,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.activateProvider(
                providerCode,
                principal.getId()
        );
    }

    @GetMapping("/provider-profiles")
    public List<PaymentProviderProfileResponse> listProfiles() {
        return service.listProfiles();
    }

    @GetMapping("/provider-profiles/{id}")
    public PaymentProviderProfileResponse getProfile(
            @PathVariable UUID id
    ) {
        return service.getProfile(id);
    }

    @PostMapping("/provider-profiles")
    public PaymentProviderProfileResponse create(
            @Valid @RequestBody PaymentProviderProfileRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.create(
                request,
                principal.getId()
        );
    }

    @PutMapping("/provider-profiles/{id}")
    public PaymentProviderProfileResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentProviderProfileRequest request,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.update(
                id,
                request,
                principal.getId()
        );
    }

    @PostMapping("/provider-profiles/{id}/activate")
    public PaymentProviderProfileResponse activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.activate(
                id,
                principal.getId()
        );
    }

    @PostMapping("/provider-profiles/{id}/test")
    public PaymentProviderTestResponse test(
            @PathVariable UUID id,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.test(
                id,
                principal.getId()
        );
    }

    @GetMapping("/provider-status")
    public PaymentProviderStatusResponse status() {
        return service.status();
    }
}
