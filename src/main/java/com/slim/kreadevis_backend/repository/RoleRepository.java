package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import com.slim.kreadevis_backend.entity.ERole;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
