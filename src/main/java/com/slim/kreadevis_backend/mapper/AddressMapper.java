package com.slim.kreadevis_backend.mapper;

import com.slim.kreadevis_backend.dto.address.AddressRequest;
import com.slim.kreadevis_backend.dto.address.AddressResponse;
import com.slim.kreadevis_backend.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressResponse toResponse(Address address);

    @Mapping(target = "id", ignore = true)
    Address toEntity(AddressRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(AddressRequest request, @MappingTarget Address address);
}
