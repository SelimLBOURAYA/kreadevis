package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.client.ClientRequest;
import com.slim.kreadevis_backend.dto.client.ClientResponse;

import java.util.List;

public interface ClientService {
    List<ClientResponse> findAll();
    ClientResponse findById(Long id);
    ClientResponse create(ClientRequest request);
    ClientResponse update(Long id, ClientRequest request);
    void delete(Long id);
}
