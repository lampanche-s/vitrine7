package br.com.vitrine7.receipt.controller;

import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.service.ReceiptHtmlRenderer;
import br.com.vitrine7.receipt.service.ReceiptService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkouts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ReceiptHtmlRenderer htmlRenderer;

    @GetMapping("/{checkoutId}/receipt")
    public ReceiptResponse receipt(
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return receiptService.getReceipt(
                checkoutId,
                principal
        );
    }

    @GetMapping(
            value = "/{checkoutId}/receipt/print",
            produces = "text/html; charset=UTF-8"
    )
    public ResponseEntity<String> print(
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        ReceiptResponse receipt =
                receiptService.getReceipt(
                        checkoutId,
                        principal
                );

        String filename =
                "recibo-nao-fiscal-" + checkoutId + ".html";

        return ResponseEntity.ok()
                .contentType(new MediaType(
                        "text",
                        "html",
                        StandardCharsets.UTF_8
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .inline()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(htmlRenderer.render(receipt));
    }
}
