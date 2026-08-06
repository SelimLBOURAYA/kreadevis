package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.dto.professional.ProfessionalRequest;
import com.slim.kreadevis_backend.dto.professional.ProfessionalResponse;
import com.slim.kreadevis_backend.service.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @GetMapping
    public ResponseEntity<List<ProfessionalResponse>> getAll() {
        return ResponseEntity.ok(professionalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(professionalService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse response = professionalService.create(request);
        return ResponseEntity.created(URI.create("/api/professionals/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> update(@PathVariable Long id, @Valid @RequestBody ProfessionalRequest request) {
        return ResponseEntity.ok(professionalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        professionalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
