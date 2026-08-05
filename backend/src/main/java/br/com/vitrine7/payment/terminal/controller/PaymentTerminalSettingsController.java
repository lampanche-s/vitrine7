package br.com.vitrine7.payment.terminal.controller;

import br.com.vitrine7.payment.terminal.dto.PaymentTerminalSettingsResponse;
import br.com.vitrine7.payment.terminal.dto.UpdatePaymentTerminalSettingsRequest;
import br.com.vitrine7.payment.terminal.service.PaymentTerminalSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-terminal/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:payment-config')")
public class PaymentTerminalSettingsController {

    private final PaymentTerminalSettingsService settingsService;

    @GetMapping
    public PaymentTerminalSettingsResponse getCurrent() {
        return settingsService.getCurrent();
    }

    @PutMapping
    public PaymentTerminalSettingsResponse update(
            @Valid
            @RequestBody
            UpdatePaymentTerminalSettingsRequest request
    ) {
        return settingsService.update(request);
    }
}
