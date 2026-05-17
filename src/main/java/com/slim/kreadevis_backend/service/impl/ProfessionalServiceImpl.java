package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.professional.ProfessionalRequest;
import com.slim.kreadevis_backend.dto.professional.ProfessionalResponse;
import com.slim.kreadevis_backend.entity.Professional;
import com.slim.kreadevis_backend.mapper.ProfessionalMapper;
import com.slim.kreadevis_backend.repository.ProfessionalRepository;
import com.slim.kreadevis_backend.service.ProfessionalService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalServiceImpl implements ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalMapper professionalMapper;

    @Override
    public List<ProfessionalResponse> findAll() {
        return professionalRepository.findAll().stream().map(professionalMapper::toResponse).toList();
    }

    @Override
    public ProfessionalResponse findById(Long id) {
        return professionalMapper.toResponse(professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional not found: " + id)));
    }

    @Override
    public ProfessionalResponse create(ProfessionalRequest request) {
        return professionalMapper.toResponse(professionalRepository.save(professionalMapper.toEntity(request)));
    }

    @Override
    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional not found: " + id));
        professional.setFirstName(request.firstName());
        professional.setLastName(request.lastName());
        professional.setPhone(request.phone());
        professional.setContactEmail(request.contactEmail());
        professional.setStreetNumber(request.streetNumber());
        professional.setStreet(request.street());
        professional.setZipCode(request.zipCode());
        professional.setCity(request.city());
        professional.setCompany(request.company());
        professional.setVat(request.vat());
        professional.setSiren(request.siren());
        return professionalMapper.toResponse(professionalRepository.save(professional));
    }

    @Override
    public void delete(Long id) {
        professionalRepository.deleteById(id);
    }
}
