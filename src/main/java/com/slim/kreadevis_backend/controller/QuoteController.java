package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.dto.quote.QuoteItemRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteItemResponse;
import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.service.QuoteItemService;
import com.slim.kreadevis_backend.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;
    private final QuoteItemService quoteItemService;

    @GetMapping("/api/quotes")
    public ResponseEntity<List<QuoteResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(quoteService.findAll(startDate, endDate));
    }

    @GetMapping("/api/quotes/{id}")
    public ResponseEntity<QuoteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.findById(id));
    }

    @GetMapping("/api/quotes/search")
    public ResponseEntity<QuoteResponse> getByReferenceCode(@RequestParam String ref) {
        return ResponseEntity.ok(quoteService.findByReferenceCode(ref));
    }

    @GetMapping("/api/clients/{clientId}/quotes")
    public ResponseEntity<List<QuoteResponse>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(quoteService.findByClientId(clientId));
    }

    @PostMapping("/api/quotes")
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(quoteService.create(request));
    }

    @PostMapping("/api/quotes/{id}/finalize")
    public ResponseEntity<QuoteResponse> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.finalize(id));
    }

    @PostMapping("/api/quotes/{id}/pending")
    public ResponseEntity<QuoteResponse> pending(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.pending(id));
    }

    @PostMapping("/api/quotes/{id}/cancel")
    public ResponseEntity<QuoteResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.cancel(id));
    }

    @DeleteMapping("/api/quotes/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/quotes/{id}/items")
    public ResponseEntity<QuoteItemResponse> addItem(@PathVariable Long id, @Valid @RequestBody QuoteItemRequest request) {
        return ResponseEntity.ok(quoteItemService.addItem(id, request));
    }

    @PutMapping("/api/quotes/{id}/items/{itemId}")
    public ResponseEntity<QuoteItemResponse> updateItem(@PathVariable Long id, @PathVariable Long itemId,
                                                        @Valid @RequestBody QuoteItemRequest request) {
        return ResponseEntity.ok(quoteItemService.updateItem(id, itemId, request));
    }

    @DeleteMapping("/api/quotes/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        quoteItemService.deleteItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
