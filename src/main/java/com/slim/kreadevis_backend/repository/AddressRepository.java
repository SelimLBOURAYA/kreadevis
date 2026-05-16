package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
