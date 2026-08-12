package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.client.ClientRequest;
import com.slim.kreadevis_backend.dto.client.ClientResponse;
import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.mapper.ClientMapper;
import com.slim.kreadevis_backend.repository.AddressRepository;
import com.slim.kreadevis_backend.repository.ClientRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
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
    private final SecurityUtils securityUtils;

    @Override
    public Page<ClientResponse> findAll(String search, Pageable pageable) {
        Page<Client> clients = securityUtils.resolveOwned(
                () -> clientRepository.search(search, pageable),
                ownerId -> clientRepository.searchByOwner(search, ownerId, pageable));
        return clients.map(clientMapper::toResponse);
    }

    @Override
    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(getOwnedClient(id));
    }

    @Override
    public ClientResponse create(ClientRequest request) {
        Address address = addressRepository.findByIdAndActiveTrue(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + request.addressId()));
        Client client = clientMapper.toEntity(request);
        client.setAddress(address);
        client.setCreatedBy(securityUtils.getCurrentUser());
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = getOwnedClient(id);
        Address address = addressRepository.findByIdAndActiveTrue(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + request.addressId()));
        clientMapper.updateEntity(request, client);
        client.setAddress(address);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    public void delete(Long id) {
        Client client = getOwnedClient(id);
        client.setActive(false);
        clientRepository.save(client);
    }

    /** Owner-scoped lookup: ROLE_ADMIN bypasses the ownership filter, everyone else only sees their own clients. */
    private Client getOwnedClient(Long id) {
        return securityUtils.resolveOwned(
                        () -> clientRepository.findByIdAndActiveTrue(id),
                        ownerId -> clientRepository.findByIdAndActiveTrueAndCreatedById(id, ownerId))
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
    }
}
