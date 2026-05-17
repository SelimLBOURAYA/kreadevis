package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.product.ProductRequest;
import com.slim.kreadevis_backend.dto.product.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> findAll();
    ProductResponse findById(Long id);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
}
