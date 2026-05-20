package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByActiveTrue();

    Optional<Client> findByIdAndActiveTrue(Long id);
}
