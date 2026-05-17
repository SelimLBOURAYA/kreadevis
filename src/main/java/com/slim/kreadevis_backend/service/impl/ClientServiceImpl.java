package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.client.ClientRequest;
import com.slim.kreadevis_backend.dto.client.ClientResponse;
import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.mapper.ClientMapper;
import com.slim.kreadevis_backend.repository.AddressRepository;
import com.slim.kreadevis_backend.repository.ClientRepository;
import com.slim.kreadevis_backend.service.ClientService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final ClientMapper clientMapper;

    @Override
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream().map(clientMapper::toResponse).toList();
    }

    @Override
    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id)));
    }

    @Override
    public ClientResponse create(ClientRequest request) {
        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + request.addressId()));
        Client client = clientMapper.toEntity(request);
        client.setAddress(address);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + request.addressId()));
        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setCompany(request.company());
        client.setSiret(request.siret());
        client.setSiren(request.siren());
        client.setVatCode(request.vatCode());
        client.setPhone(request.phone());
        client.setEmail(request.email());
        client.setAddress(address);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        client.setDeleted(true);
        clientRepository.save(client);
    }
}
