package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.product.ProductRequest;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Professional;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProfessionalRepository;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProfessionalRepository professionalRepository;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    @Override
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id)));
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (request.supplierId() != null) {
            Professional supplier = professionalRepository.findById(request.supplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Professional not found: " + request.supplierId()));
            product.setSupplier(supplier);
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setLabel(request.label());
        product.setDescription(request.description());
        product.setStockQuantity(request.stockQuantity());
        product.setUnitPrice(request.unitPrice());
        product.setVatRate(request.vatRate());
        product.setReferenceCode(request.referenceCode());
        if (request.supplierId() != null) {
            Professional supplier = professionalRepository.findById(request.supplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Professional not found: " + request.supplierId()));
            product.setSupplier(supplier);
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setDeleted(true);
        productRepository.save(product);
    }
}
