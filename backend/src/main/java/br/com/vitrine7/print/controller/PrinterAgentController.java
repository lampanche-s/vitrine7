package br.com.vitrine7.print.controller;

import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.print.security.PrinterAgentProperties;
import br.com.vitrine7.print.service.PrintJobService;
import br.com.vitrine7.print.service.PrinterAgentHealthService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/printer-agent")
@Validated
@PreAuthorize("hasAuthority('printer-agent:bridge')")
public class PrinterAgentController {

    public static final String JOB_ID_HEADER = "X-Print-Job-Id";
    public static final String ATTEMPT_HEADER = "X-Print-Attempt";

    private final PrintJobService service;
    private final PrinterAgentProperties properties;
    private final PrinterAgentHealthService healthService;

    public PrinterAgentController(
            PrintJobService service,
            PrinterAgentProperties properties,
            PrinterAgentHealthService healthService
    ) {
        this.service = service;
        this.properties = properties;
        this.healthService = healthService;
    }

    @GetMapping(
            value = "/jobs/next",
            produces = "text/plain; charset=UTF-8"
    )
    public ResponseEntity<String> next(
            @RequestParam(defaultValue = "20") @Min(0) @Max(25) int waitSeconds,
            @RequestHeader(
                    value = "X-Printer-Agent-Identity",
                    required = false
            ) @Size(max = 120) String identity,
            @RequestHeader(
                    value = "X-Printer-Agent-Version",
                    required = false
            ) @Size(max = 60) String agentVersion
    ) {
        healthService.heartbeat(identity, agentVersion);
        Duration requested = Duration.ofSeconds(waitSeconds);
        Duration configured = properties.longPollMax() == null
                ? Duration.ofSeconds(25)
                : properties.longPollMax();
        Duration wait = requested.compareTo(configured) > 0
                ? configured
                : requested;
        Instant deadline = Instant.now().plus(wait);

        do {
            PrintJobDtos.Delivery delivery = service.reserveNext();
            if (delivery != null) {
                MediaType contentType = new MediaType(
                        "text",
                        "plain",
                        StandardCharsets.UTF_8
                );

                return ResponseEntity.ok()
                        .contentType(contentType)
                        .header(JOB_ID_HEADER, delivery.id().toString())
                        .header(ATTEMPT_HEADER, String.valueOf(delivery.attempt()))
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .body(delivery.receiptText());
            }

            if (!Instant.now().isBefore(deadline)) {
                break;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (true);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/heartbeat")
    public PrinterAgentHealthService.Heartbeat heartbeat(
            @RequestParam(required = false) @Size(max = 120) String identity,
            @RequestParam(required = false) @Size(max = 60) String agentVersion
    ) {
        return healthService.heartbeat(identity, agentVersion);
    }

    @PostMapping(
            value = "/jobs/{id}/result",
            consumes = MediaType.TEXT_PLAIN_VALUE
    )
    public PrintJobDtos.ResultResponse result(
            @PathVariable UUID id,
            @RequestParam boolean success,
            @RequestBody(required = false) String errorMessage
    ) {
        return service.complete(
                id,
                new PrintJobDtos.ResultRequest(success, errorMessage)
        );
    }
}
