package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
