package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.product.ProductRequest;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Professional;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProfessionalRepository;
import com.slim.kreadevis_backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private ProductMapper productMapper;
    @InjectMocks private ProductServiceImpl productService;

    @Test
    void findAll_shouldReturnMappedList() {
        Product product = new Product();
        ProductResponse response = dummyResponse();
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        List<ProductResponse> result = productService.findAll();

        assertThat(result).hasSize(1).contains(response);
    }

    @Test
    void findById_shouldReturnResponse_whenFound() {
        Product product = new Product();
        ProductResponse response = dummyResponse();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Product not found: 99");
    }

    @Test
    void create_shouldSaveWithoutSupplier_whenSupplierIdNull() {
        ProductRequest request = requestWithoutSupplier();
        Product entity = new Product();
        ProductResponse response = dummyResponse();
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(response);
        verifyNoInteractions(professionalRepository);
    }

    @Test
    void create_shouldSaveWithSupplier_whenSupplierIdSet() {
        ProductRequest request = requestWithSupplier();
        Product entity = spy(new Product());
        Professional supplier = new Professional();
        ProductResponse response = dummyResponse();
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(professionalRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(supplier));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(response);
        verify(entity).setSupplier(supplier);
    }

    @Test
    void create_shouldThrow_whenSupplierNotFound() {
        ProductRequest request = requestWithSupplier();
        when(productMapper.toEntity(request)).thenReturn(new Product());
        when(professionalRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Professional not found: 5");
    }

    @Test
    void update_shouldApplyAndSave_whenFound() {
        ProductRequest request = requestWithoutSupplier();
        Product product = new Product();
        ProductResponse response = dummyResponse();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(productMapper).updateEntity(request, product);
    }

    @Test
    void update_shouldUpdateSupplier_whenSupplierIdSet() {
        ProductRequest request = requestWithSupplier();
        Product product = spy(new Product());
        Professional supplier = new Professional();
        ProductResponse response = dummyResponse();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(professionalRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(supplier));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.update(1L, request);

        verify(product).setSupplier(supplier);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, requestWithoutSupplier()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldDeactivateAndSave_whenFound() {
        Product product = new Product();
        product.setActive(true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private ProductRequest requestWithoutSupplier() {
        return new ProductRequest("Widget", "A thing", 100L, 9.99f, 0.20f, "WID-001", null);
    }

    private ProductRequest requestWithSupplier() {
        return new ProductRequest("Widget", "A thing", 100L, 9.99f, 0.20f, "WID-001", 5L);
    }

    private ProductResponse dummyResponse() {
        return new ProductResponse(1L, "Widget", "A thing", 100L, 9.99f, 0.20f, "WID-001", null);
    }
}
