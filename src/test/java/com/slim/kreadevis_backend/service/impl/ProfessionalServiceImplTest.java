package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.professional.ProfessionalRequest;
import com.slim.kreadevis_backend.dto.professional.ProfessionalResponse;
import com.slim.kreadevis_backend.entity.Professional;
import com.slim.kreadevis_backend.mapper.ProfessionalMapper;
import com.slim.kreadevis_backend.repository.ProfessionalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceImplTest {

    @Mock private ProfessionalRepository professionalRepository;
    @Mock private ProfessionalMapper professionalMapper;
    @InjectMocks private ProfessionalServiceImpl professionalService;

    @Test
    void findAll_shouldReturnMappedList() {
        Professional professional = new Professional();
        ProfessionalResponse response = dummyResponse();
        when(professionalRepository.findAllByActiveTrue()).thenReturn(List.of(professional));
        when(professionalMapper.toResponse(professional)).thenReturn(response);

        List<ProfessionalResponse> result = professionalService.findAll();

        assertThat(result).hasSize(1).contains(response);
    }

    @Test
    void findById_shouldReturnResponse_whenFound() {
        Professional professional = new Professional();
        ProfessionalResponse response = dummyResponse();
        when(professionalRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(professional));
        when(professionalMapper.toResponse(professional)).thenReturn(response);

        ProfessionalResponse result = professionalService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(professionalRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Professional not found: 99");
    }

    @Test
    void create_shouldSaveAndReturnResponse() {
        ProfessionalRequest request = dummyRequest();
        Professional entity = new Professional();
        ProfessionalResponse response = dummyResponse();
        when(professionalMapper.toEntity(request)).thenReturn(entity);
        when(professionalRepository.save(entity)).thenReturn(entity);
        when(professionalMapper.toResponse(entity)).thenReturn(response);

        ProfessionalResponse result = professionalService.create(request);

        assertThat(result).isEqualTo(response);
        verify(professionalRepository).save(entity);
    }

    @Test
    void update_shouldApplyAndSave_whenFound() {
        ProfessionalRequest request = dummyRequest();
        Professional professional = new Professional();
        ProfessionalResponse response = dummyResponse();
        when(professionalRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(professional)).thenReturn(professional);
        when(professionalMapper.toResponse(professional)).thenReturn(response);

        ProfessionalResponse result = professionalService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(professionalMapper).updateEntity(request, professional);
        verify(professionalRepository).save(professional);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(professionalRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.update(99L, dummyRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldDeactivateAndSave_whenFound() {
        Professional professional = new Professional();
        professional.setActive(true);
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));

        professionalService.delete(1L);

        assertThat(professional.isActive()).isFalse();
        verify(professionalRepository).save(professional);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private ProfessionalRequest dummyRequest() {
        return new ProfessionalRequest("Jane", "Smith", "0123456789", "jane@corp.com", "10", "Main St", "75001", "Paris", "Corp", "FR123", "5678");
    }

    private ProfessionalResponse dummyResponse() {
        return new ProfessionalResponse(1L, "Jane", "Smith", "Corp", "0123456789", "jane@corp.com");
    }
}
