package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.CsvImportColumns;
import com.slim.kreadevis_backend.dto.product.CsvImportResult;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;

    private CsvImportServiceImpl csvImportService;

    @BeforeEach
    void setUp() {
        CsvImportColumns defaultColumns = new CsvImportColumns(
                "label", "description", "stockQuantity", "unitPrice", "vatRate", "referenceCode");
        csvImportService = new CsvImportServiceImpl(productRepository, productMapper, defaultColumns);
    }

    @Test
    void importProducts_importsValidRows() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Robinet,Mitigeur,10,85.0,20.0,REF-001\n" +
                     "Tuyau,Cuivre,50,12.5,20.0,REF-002\n";
        MockMultipartFile file = multipartFile(csv);

        when(productRepository.findByReferenceCodeAndActiveTrue(anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenAnswer(inv -> null);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    void importProducts_reportsErrorForMissingLabel() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     ",No label,5,10.0,20.0,REF-003\n";
        MockMultipartFile file = multipartFile(csv);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).containsIgnoringCase("label");
        verify(productRepository, never()).save(any());
    }

    @Test
    void importProducts_reportsErrorForInvalidStockQuantity() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Robinet,Desc,abc,85.0,20.0,REF-004\n";
        MockMultipartFile file = multipartFile(csv);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).containsIgnoringCase("stockQuantity");
    }

    @Test
    void importProducts_reportsErrorForInvalidUnitPrice() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Robinet,Desc,5,not-a-price,20.0,REF-005\n";
        MockMultipartFile file = multipartFile(csv);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).containsIgnoringCase("unitPrice");
    }

    @Test
    void importProducts_reportsErrorForInvalidVatRate() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Robinet,Desc,5,85.0,bad-vat,REF-006\n";
        MockMultipartFile file = multipartFile(csv);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).containsIgnoringCase("vatRate");
    }

    @Test
    void importProducts_updatesExistingProduct_whenReferenceCodeExists() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Updated Name,New Desc,10,99.0,20.0,EXISTING\n";
        MockMultipartFile file = multipartFile(csv);

        Product existing = new Product();
        when(productRepository.findByReferenceCodeAndActiveTrue("EXISTING")).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(existing)).thenAnswer(inv -> null);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.warnings()).isEmpty();
        verify(productRepository).save(existing);
        assertThat(existing.getLabel()).isEqualTo("Updated Name");
        assertThat(existing.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void importProducts_importsRowWithBlankReferenceCode() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Robinet,Desc,5,85.0,20.0,\n";
        MockMultipartFile file = multipartFile(csv);

        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenAnswer(inv -> null);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        verify(productRepository, never()).findByReferenceCodeAndActiveTrue(any());
    }

    @Test
    void importProducts_handlesEmptyCsvBody() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n";
        MockMultipartFile file = multipartFile(csv);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void importProducts_handlesColumnsInDifferentOrder() {
        String csv = "referenceCode,vatRate,unitPrice,label,stockQuantity,description\n" +
                     "REF-010,10.0,75.0,Robinet,5,Desc different order\n";
        MockMultipartFile file = multipartFile(csv);

        when(productRepository.findByReferenceCodeAndActiveTrue("REF-010")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenAnswer(inv -> null);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void importProducts_throwsOnMissingExpectedColumn() {
        String csv = "label,description,stockQuantity,vatRate,referenceCode\n" +
                     "Robinet,Desc,5,20.0,REF-011\n";
        MockMultipartFile file = multipartFile(csv);

        assertThatThrownBy(() -> csvImportService.importProducts(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitPrice")
                .hasMessageContaining("missing expected columns");
    }

    @Test
    void importProducts_mixedValidAndInvalidRows() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Valid Product,Desc,10,50.0,20.0,REF-OK\n" +
                     ",Missing label,5,10.0,20.0,REF-ERR\n" +
                     "Another Product,Desc,3,30.0,10.0,\n";
        MockMultipartFile file = multipartFile(csv);

        when(productRepository.findByReferenceCodeAndActiveTrue("REF-OK")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenAnswer(inv -> null);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).lineNumber()).isEqualTo(3);
    }

    private MockMultipartFile multipartFile(String csv) {
        return new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
