package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    Professional findByContactEmail(String contactEmail);
}
