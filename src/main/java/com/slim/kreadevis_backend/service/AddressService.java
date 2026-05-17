package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.address.AddressRequest;
import com.slim.kreadevis_backend.dto.address.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> findAll();
    AddressResponse findById(Long id);
    AddressResponse create(AddressRequest request);
    AddressResponse update(Long id, AddressRequest request);
    void delete(Long id);
}
