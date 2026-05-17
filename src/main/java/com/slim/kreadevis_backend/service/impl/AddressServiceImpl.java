package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.address.AddressRequest;
import com.slim.kreadevis_backend.dto.address.AddressResponse;
import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.mapper.AddressMapper;
import com.slim.kreadevis_backend.repository.AddressRepository;
import com.slim.kreadevis_backend.service.AddressService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Override
    public List<AddressResponse> findAll() {
        return addressRepository.findAll().stream().map(addressMapper::toResponse).toList();
    }

    @Override
    public AddressResponse findById(Long id) {
        return addressMapper.toResponse(addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + id)));
    }

    @Override
    public AddressResponse create(AddressRequest request) {
        return addressMapper.toResponse(addressRepository.save(addressMapper.toEntity(request)));
    }

    @Override
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + id));
        address.setStreetNumber(request.streetNumber());
        address.setStreet(request.street());
        address.setPostalCode(request.postalCode());
        address.setCity(request.city());
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public void delete(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + id));
        address.setDeleted(true);
        addressRepository.save(address);
    }
}
