package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    @Query("SELECT c FROM Client c WHERE c.active = true "
            + "AND (:search IS NULL OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Client> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.active = true AND c.createdBy.id = :ownerId "
            + "AND (:search IS NULL OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Client> searchByOwner(@Param("search") String search, @Param("ownerId") Long ownerId, Pageable pageable);

    Optional<Client> findByIdAndActiveTrue(Long id);

    Optional<Client> findByIdAndActiveTrueAndCreatedById(Long id, Long ownerId);
}
