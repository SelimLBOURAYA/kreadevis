package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrue();

    Optional<Product> findByIdAndActiveTrue(Long id);
}
