package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.client.ClientRequest;
import com.slim.kreadevis_backend.dto.client.ClientResponse;
import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.ClientMapper;
import com.slim.kreadevis_backend.repository.AddressRepository;
import com.slim.kreadevis_backend.repository.ClientRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ClientMapper clientMapper;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private ClientServiceImpl clientService;

    private static final Long ADDRESS_ID = 10L;
    private static final Long OWNER_ID = 42L;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(OWNER_ID);
    }

    @Test
    void findAll_shouldReturnMappedPage_scopedToOwner_whenNotAdmin() {
        Client client = new Client();
        ClientResponse response = dummyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.searchByOwner(null, OWNER_ID, pageable)).thenReturn(new PageImpl<>(List.of(client)));
        when(clientMapper.toResponse(client)).thenReturn(response);

        Page<ClientResponse> result = clientService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1).contains(response);
    }

    @Test
    void findAll_shouldReturnAllClients_whenAdmin() {
        Client client = new Client();
        ClientResponse response = dummyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(clientRepository.search(null, pageable)).thenReturn(new PageImpl<>(List.of(client)));
        when(clientMapper.toResponse(client)).thenReturn(response);

        Page<ClientResponse> result = clientService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1).contains(response);
        verify(clientRepository, never()).searchByOwner(any(), any(), any());
    }

    @Test
    void findById_shouldReturnResponse_whenOwnedByCurrentUser() {
        Client client = new Client();
        ClientResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Client not found: 1");
    }

    @Test
    void findById_shouldBypassOwnership_whenAdmin() {
        Client client = new Client();
        ClientResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(true);
        when(clientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.findById(1L);

        assertThat(result).isEqualTo(response);
        verify(clientRepository, never()).findByIdAndActiveTrueAndCreatedById(any(), any());
    }

    @Test
    void create_shouldSaveWithCurrentUserAsCreatedBy() {
        ClientRequest request = requestWithAddress();
        Address address = new Address();
        Client client = new Client();
        Client saved = new Client();
        ClientResponse response = dummyResponse();
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(addressRepository.findByIdAndActiveTrue(ADDRESS_ID)).thenReturn(Optional.of(address));
        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(saved);
        when(clientMapper.toResponse(saved)).thenReturn(response);

        ClientResponse result = clientService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(client.getCreatedBy()).isEqualTo(currentUser);
        verify(clientRepository).save(client);
    }

    @Test
    void create_shouldThrow_whenAddressNotFound() {
        ClientRequest request = requestWithAddress();
        when(addressRepository.findByIdAndActiveTrue(ADDRESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Address not found: " + ADDRESS_ID);
    }

    @Test
    void update_shouldApplyAndSave_whenOwnedByCurrentUser() {
        ClientRequest request = requestWithAddress();
        Address address = new Address();
        Client client = new Client();
        ClientResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(client));
        when(addressRepository.findByIdAndActiveTrue(ADDRESS_ID)).thenReturn(Optional.of(address));
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(clientMapper).updateEntity(request, client);
        verify(clientRepository).save(client);
    }

    @Test
    void update_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.update(99L, requestWithAddress()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_shouldThrow_whenAddressNotFound() {
        Client client = new Client();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(client));
        when(addressRepository.findByIdAndActiveTrue(ADDRESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.update(1L, requestWithAddress()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldDeactivateAndSave_whenOwnedByCurrentUser() {
        Client client = new Client();
        client.setActive(true);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(client));

        clientService.delete(1L);

        assertThat(client.isActive()).isFalse();
        verify(clientRepository).save(client);
    }

    @Test
    void delete_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private ClientRequest requestWithAddress() {
        return new ClientRequest("John", "Doe", "Acme", "1234", "5678", "FR123", "0123456789", "john@acme.com", ADDRESS_ID);
    }

    private ClientResponse dummyResponse() {
        return new ClientResponse(1L, "John", "Doe", "Acme", "john@acme.com", "0123456789", null);
    }
}
