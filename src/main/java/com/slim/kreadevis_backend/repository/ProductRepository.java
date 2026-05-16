package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByLabel(String label);

    Optional<Product> findByReferenceCode(String referenceCode);
}
