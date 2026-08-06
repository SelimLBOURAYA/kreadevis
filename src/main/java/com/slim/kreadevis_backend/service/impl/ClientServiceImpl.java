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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final ClientMapper clientMapper;

    @Override
    public Page<ClientResponse> findAll(String search, Pageable pageable) {
        return clientRepository.search(search, pageable).map(clientMapper::toResponse);
    }

    @Override
    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(clientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id)));
    }

    @Override
    public ClientResponse create(ClientRequest request) {
        Address address = addressRepository.findByIdAndActiveTrue(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + request.addressId()));
        Client client = clientMapper.toEntity(request);
        client.setAddress(address);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        Address address = addressRepository.findByIdAndActiveTrue(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + request.addressId()));
        clientMapper.updateEntity(request, client);
        client.setAddress(address);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        client.setActive(false);
        clientRepository.save(client);
    }
}
