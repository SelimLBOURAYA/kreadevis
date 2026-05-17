package com.slim.kreadevis_backend.mapper;

import com.slim.kreadevis_backend.dto.professional.ProfessionalRequest;
import com.slim.kreadevis_backend.dto.professional.ProfessionalResponse;
import com.slim.kreadevis_backend.entity.Professional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfessionalMapper {
    ProfessionalResponse toResponse(Professional professional);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Professional toEntity(ProfessionalRequest request);
}
