package br.com.vitrine7.operations;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/agents")
@RequiredArgsConstructor
public class AgentOperationalHealthController {
    private final AgentOperationalHealthService service;

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('operations:agents-status','admin:payment-config')")
    public AgentOperationalHealthResponse status() {
        return service.status();
    }
}
