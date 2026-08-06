package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.product.ProductRequest;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Professional;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProfessionalRepository;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private ProductServiceImpl productService;

    private static final Long OWNER_ID = 42L;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(OWNER_ID);
    }

    @Test
    void findAll_shouldReturnMappedPage_scopedToOwner_whenNotAdmin() {
        Product product = new Product();
        ProductResponse response = dummyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.searchByOwner(null, OWNER_ID, pageable)).thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1).contains(response);
    }

    @Test
    void findAll_shouldReturnAllProducts_whenAdmin() {
        Product product = new Product();
        ProductResponse response = dummyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(productRepository.search(null, pageable)).thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1).contains(response);
        verify(productRepository, never()).searchByOwner(any(), any(), any());
    }

    @Test
    void findById_shouldReturnResponse_whenOwnedByCurrentUser() {
        Product product = new Product();
        ProductResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Product not found: 99");
    }

    @Test
    void findById_shouldBypassOwnership_whenAdmin() {
        Product product = new Product();
        ProductResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(true);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.findById(1L);

        assertThat(result).isEqualTo(response);
        verify(productRepository, never()).findByIdAndActiveTrueAndCreatedById(any(), any());
    }

    @Test
    void create_shouldSaveWithoutSupplier_whenSupplierIdNull() {
        ProductRequest request = requestWithoutSupplier();
        Product entity = new Product();
        ProductResponse response = dummyResponse();
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getCreatedBy()).isEqualTo(currentUser);
        verifyNoInteractions(professionalRepository);
    }

    @Test
    void create_shouldSaveWithSupplier_whenSupplierIdSet() {
        ProductRequest request = requestWithSupplier();
        Product entity = new Product();
        Professional supplier = new Professional();
        ProductResponse response = dummyResponse();
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(professionalRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(supplier));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(response);
        verify(productRepository).save(entity);
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
    void update_shouldApplyAndSave_whenOwnedByCurrentUser() {
        ProductRequest request = requestWithoutSupplier();
        Product product = new Product();
        ProductResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(productMapper).updateEntity(request, product);
    }

    @Test
    void update_shouldUpdateSupplier_whenSupplierIdSet() {
        ProductRequest request = requestWithSupplier();
        Product product = new Product();
        Professional supplier = new Professional();
        ProductResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(product));
        when(professionalRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(supplier));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.update(1L, request);

        verify(productMapper).updateEntity(request, product);
        verify(productRepository).save(product);
    }

    @Test
    void update_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, requestWithoutSupplier()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldDeactivateAndSave_whenOwnedByCurrentUser() {
        Product product = new Product();
        product.setActive(true);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(product));

        productService.delete(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void delete_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(productRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private ProductRequest requestWithoutSupplier() {
        return new ProductRequest("Widget", "A thing", 100L, new BigDecimal("9.99"), new BigDecimal("0.20"), "WID-001", null);
    }

    private ProductRequest requestWithSupplier() {
        return new ProductRequest("Widget", "A thing", 100L, new BigDecimal("9.99"), new BigDecimal("0.20"), "WID-001", 5L);
    }

    private ProductResponse dummyResponse() {
        return new ProductResponse(1L, "Widget", "A thing", 100L, new BigDecimal("9.99"), new BigDecimal("0.20"), "WID-001", null);
    }
}
