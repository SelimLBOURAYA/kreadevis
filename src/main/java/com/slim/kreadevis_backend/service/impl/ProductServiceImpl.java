package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.product.ProductRequest;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Professional;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProfessionalRepository;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProfessionalRepository professionalRepository;
    private final ProductMapper productMapper;
    private final SecurityUtils securityUtils;

    @Override
    public Page<ProductResponse> findAll(String search, Pageable pageable) {
        Page<Product> products = securityUtils.resolveOwned(
                () -> productRepository.search(search, pageable),
                ownerId -> productRepository.searchByOwner(search, ownerId, pageable));
        return products.map(productMapper::toResponse);
    }

    @Override
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getOwnedProduct(id));
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (request.supplierId() != null) {
            Professional supplier = professionalRepository.findByIdAndActiveTrue(request.supplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Professional not found: " + request.supplierId()));
            product.setSupplier(supplier);
        }
        product.setCreatedBy(securityUtils.getCurrentUser());
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getOwnedProduct(id);
        productMapper.updateEntity(request, product);
        if (request.supplierId() != null) {
            Professional supplier = professionalRepository.findByIdAndActiveTrue(request.supplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Professional not found: " + request.supplierId()));
            product.setSupplier(supplier);
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        Product product = getOwnedProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    /** Owner-scoped lookup: ROLE_ADMIN bypasses the ownership filter, everyone else only sees their own products. */
    private Product getOwnedProduct(Long id) {
        return securityUtils.resolveOwned(
                        () -> productRepository.findByIdAndActiveTrue(id),
                        ownerId -> productRepository.findByIdAndActiveTrueAndCreatedById(id, ownerId))
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }
}
