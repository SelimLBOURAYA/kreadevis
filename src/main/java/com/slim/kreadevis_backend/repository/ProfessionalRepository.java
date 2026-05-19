package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    List<Professional> findAllByActiveTrue();

    Optional<Professional> findByIdAndActiveTrue(Long id);
}
