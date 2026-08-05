package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.payment.terminal.bridge.dto.TerminalCommandDtos;
import br.com.vitrine7.payment.terminal.bridge.dto.TerminalDeviceDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-terminal/bridge")
@RequiredArgsConstructor
@Validated
public class TerminalBridgeController {
    private final TerminalDeviceService devices;
    private final TerminalCommandQueueService commands;
    private final TerminalBridgeProperties properties;

    @PostMapping("/pair")
    @PreAuthorize("permitAll()")
    public TerminalDeviceDtos.PairResponse pair(@Valid @RequestBody TerminalDeviceDtos.PairRequest request) {
        return devices.pair(request);
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasAuthority('payment-terminal:bridge')")
    public TerminalDeviceDtos.HeartbeatResponse heartbeat(
            @AuthenticationPrincipal TerminalDevicePrincipal principal,
            @Valid @RequestBody TerminalDeviceDtos.HeartbeatRequest request) {
        return devices.heartbeat(principal, request);
    }

    @GetMapping("/commands/next")
    @PreAuthorize("hasAuthority('payment-terminal:bridge')")
    public ResponseEntity<TerminalCommandDtos.Delivery> next(
            @AuthenticationPrincipal TerminalDevicePrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) @Max(25) int waitSeconds) {
        Duration requested = Duration.ofSeconds(waitSeconds);
        Duration wait = requested.compareTo(properties.longPollMax()) > 0 ? properties.longPollMax() : requested;
        Instant deadline = Instant.now().plus(wait);
        do {
            TerminalCommandDtos.Delivery delivery = commands.reserveNext(principal.deviceId());
            if (delivery != null) return ResponseEntity.ok(delivery);
            if (!Instant.now().isBefore(deadline)) break;
            try { Thread.sleep(200); } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); break;
            }
        } while (true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/commands/{commandId}/ack")
    @PreAuthorize("hasAuthority('payment-terminal:bridge')")
    public TerminalCommandDtos.Acknowledgement ack(@AuthenticationPrincipal TerminalDevicePrincipal principal,
                                                    @PathVariable UUID commandId) {
        return commands.acknowledge(commandId, principal.deviceId());
    }

    @PostMapping("/commands/{commandId}/result")
    @PreAuthorize("hasAuthority('payment-terminal:bridge')")
    public TerminalCommandDtos.ResultResponse result(@AuthenticationPrincipal TerminalDevicePrincipal principal,
                                                      @PathVariable UUID commandId,
                                                      @Valid @RequestBody TerminalCommandDtos.ResultRequest request) {
        return commands.submitResult(commandId, principal.deviceId(), request);
    }
}
