package br.com.vitrine7.report.controller;

import br.com.vitrine7.report.dto.ReportAccessRequest;
import br.com.vitrine7.report.service.ReportPeriodAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/access")
@PreAuthorize("hasAuthority('reports:access')")
public class ReportAccessController {

    private final ReportPeriodAccessService accessService;

    public ReportAccessController(ReportPeriodAccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@RequestBody ReportAccessRequest request) {
        if (!accessService.isPasswordValid(request.password())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.noContent().build();
    }
}
