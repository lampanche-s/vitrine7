package br.com.vitrine7.backup.controller;

import br.com.vitrine7.backup.service.SystemBackupService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/system")
@PreAuthorize("hasAuthority('reports:access')")
public class SystemBackupController {

    private final SystemBackupService backupService;

    public SystemBackupController(
            SystemBackupService backupService
    ) {
        this.backupService = backupService;
    }

    @PostMapping(
            value = "/backup",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<StreamingResponseBody> download() {
        SystemBackupService.GeneratedBackup backup =
                backupService.generate();

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream =
                         Files.newInputStream(backup.path())) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            } finally {
                backupService.delete(backup);
            }
        };

        String contentDisposition = ContentDisposition
                .attachment()
                .filename(
                        backup.fileName(),
                        StandardCharsets.UTF_8
                )
                .build()
                .toString();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .contentLength(backup.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}
