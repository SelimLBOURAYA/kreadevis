package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.address.AddressRequest;
import com.slim.kreadevis_backend.dto.address.AddressResponse;
import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.mapper.AddressMapper;
import com.slim.kreadevis_backend.repository.AddressRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private AddressMapper addressMapper;
    @InjectMocks private AddressServiceImpl addressService;

    @Test
    void findAll_shouldReturnMappedList() {
        Address address = new Address();
        AddressResponse response = new AddressResponse(1L, "12", "Main St", "75001", "Paris");
        when(addressRepository.findAllByActiveTrue()).thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(response);

        List<AddressResponse> result = addressService.findAll();

        assertThat(result).hasSize(1).contains(response);
        verify(addressRepository).findAllByActiveTrue();
    }

    @Test
    void findById_shouldReturnResponse_whenFound() {
        Address address = new Address();
        AddressResponse response = new AddressResponse(1L, "12", "Main St", "75001", "Paris");
        when(addressRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(address));
        when(addressMapper.toResponse(address)).thenReturn(response);

        AddressResponse result = addressService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(addressRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Address not found: 99");
    }

    @Test
    void create_shouldSaveAndReturnResponse() {
        AddressRequest request = new AddressRequest("12", "Main St", "75001", "Paris");
        Address entity = new Address();
        AddressResponse response = new AddressResponse(1L, "12", "Main St", "75001", "Paris");
        when(addressMapper.toEntity(request)).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressMapper.toResponse(entity)).thenReturn(response);

        AddressResponse result = addressService.create(request);

        assertThat(result).isEqualTo(response);
        verify(addressRepository).save(entity);
    }

    @Test
    void update_shouldApplyAndSave_whenFound() {
        AddressRequest request = new AddressRequest("10", "Oak Ave", "69001", "Lyon");
        Address address = new Address();
        AddressResponse response = new AddressResponse(1L, "10", "Oak Ave", "69001", "Lyon");
        when(addressRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(response);

        AddressResponse result = addressService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(addressMapper).updateEntity(request, address);
        verify(addressRepository).save(address);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(addressRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(99L, new AddressRequest("x", "y", "z", "w")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldDeactivateAndSave_whenFound() {
        Address address = new Address();
        address.setActive(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        addressService.delete(1L);

        assertThat(address.isActive()).isFalse();
        verify(addressRepository).save(address);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
