package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.dto.quote.SendQuoteRequest;
import com.slim.kreadevis_backend.dto.quote.SendQuoteResponse;
import com.slim.kreadevis_backend.service.PdfService;
import com.slim.kreadevis_backend.service.QuoteEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteDocumentController {

    private final PdfService pdfService;
    private final QuoteEmailService quoteEmailService;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getQuotePdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateQuotePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quote-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<SendQuoteResponse> sendQuote(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false) SendQuoteRequest request) {
        return ResponseEntity.ok(quoteEmailService.sendQuoteToClient(id, request));
    }
}
