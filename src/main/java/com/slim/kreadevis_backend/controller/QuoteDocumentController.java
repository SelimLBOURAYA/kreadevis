package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteDocumentController {

    private final PdfService pdfService;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getQuotePdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateQuotePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quote-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<Resource> getInvoicePdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
