package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.payment.terminal.bridge.dto.TerminalDeviceDtos;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-terminal/devices")
@PreAuthorize("hasAuthority('admin:payment-config')")
@RequiredArgsConstructor
public class TerminalDeviceAdminController {
    private final TerminalDeviceService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TerminalDeviceDtos.Response create(@Valid @RequestBody TerminalDeviceDtos.CreateRequest request,
                                               @AuthenticationPrincipal VitrineUserPrincipal principal) {
        return service.create(request, principal.getId());
    }
    @GetMapping public List<TerminalDeviceDtos.Response> list() { return service.list(); }
    @GetMapping("/{id}") public TerminalDeviceDtos.Response get(@PathVariable UUID id) { return service.require(id); }
    @PostMapping("/{id}/pairing-code")
    public TerminalDeviceDtos.PairingCodeResponse pairing(@PathVariable UUID id,
                                                           @AuthenticationPrincipal VitrineUserPrincipal principal) {
        return service.createPairingCode(id, principal.getId());
    }
    @PostMapping("/{id}/revoke") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id, @AuthenticationPrincipal VitrineUserPrincipal principal) {
        service.revoke(id, principal.getId());
    }
}
