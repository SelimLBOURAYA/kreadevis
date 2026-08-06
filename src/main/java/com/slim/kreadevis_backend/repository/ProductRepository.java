package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.active = true "
            + "AND (:search IS NULL OR LOWER(p.label) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(p.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> search(@Param("search") String search, Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    Optional<Product> findByReferenceCodeAndActiveTrue(String referenceCode);
}
