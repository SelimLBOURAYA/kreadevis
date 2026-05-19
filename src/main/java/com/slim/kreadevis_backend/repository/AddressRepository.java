package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByActiveTrue();

    Optional<Address> findByIdAndActiveTrue(Long id);
}
