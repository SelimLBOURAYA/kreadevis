package com.slim.kreadevis_backend.mapper;

import com.slim.kreadevis_backend.dto.client.ClientRequest;
import com.slim.kreadevis_backend.dto.client.ClientResponse;
import com.slim.kreadevis_backend.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface ClientMapper {
    ClientResponse toResponse(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "quotes", ignore = true)
    Client toEntity(ClientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "quotes", ignore = true)
    void updateEntity(ClientRequest request, @MappingTarget Client client);
}
