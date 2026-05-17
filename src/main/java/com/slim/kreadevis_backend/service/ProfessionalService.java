package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.professional.ProfessionalRequest;
import com.slim.kreadevis_backend.dto.professional.ProfessionalResponse;

import java.util.List;

public interface ProfessionalService {
    List<ProfessionalResponse> findAll();
    ProfessionalResponse findById(Long id);
    ProfessionalResponse create(ProfessionalRequest request);
    ProfessionalResponse update(Long id, ProfessionalRequest request);
    void delete(Long id);
}
