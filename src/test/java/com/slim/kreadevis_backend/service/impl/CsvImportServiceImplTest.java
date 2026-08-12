package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.CsvImportColumns;
import com.slim.kreadevis_backend.config.CsvImportProperties;
import com.slim.kreadevis_backend.dto.product.CsvImportResult;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceImplTest {

    private static final Long OWNER_ID = 42L;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private SecurityUtils securityUtils;

    private CsvImportServiceImpl csvImportService;

    @BeforeEach
    void setUp() {
        CsvImportColumns defaultColumns = new CsvImportColumns(
                "label", "description", "stockQuantity", "unitPrice", "vatRate", "referenceCode");
        CsvImportProperties properties = new CsvImportProperties(10_000, List.of("text/csv", "application/vnd.ms-excel"));
        csvImportService = new CsvImportServiceImpl(productRepository, productMapper, defaultColumns, properties, securityUtils);

        User currentUser = new User();
        currentUser.setId(OWNER_ID);
        lenient().when(securityUtils.isAdmin()).thenReturn(false);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(currentUser);
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

        User owner = new User();
        owner.setId(OWNER_ID);
        Product existing = new Product();
        existing.setCreatedBy(owner);
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
    void importProducts_reportsRowError_whenReferenceCodeOwnedByAnotherUser() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "New Owner Product,Desc,10,99.0,20.0,SHARED-REF\n";
        MockMultipartFile file = multipartFile(csv);

        User anotherUser = new User();
        anotherUser.setId(99L);
        Product ownedByAnotherUser = new Product();
        ownedByAnotherUser.setCreatedBy(anotherUser);
        when(productRepository.findByReferenceCodeAndActiveTrue("SHARED-REF")).thenReturn(Optional.of(ownedByAnotherUser));

        CsvImportResult result = csvImportService.importProducts(file);

        // reference_code is globally unique: creating a duplicate would violate the
        // constraint, so the row is rejected instead.
        assertThat(result.importedCount()).isZero();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("Reference code already used: SHARED-REF");
        // must not have mutated the other user's product
        assertThat(ownedByAnotherUser.getLabel()).isNull();
        verify(productRepository, never()).save(any());
    }

    @Test
    void importProducts_updatesProductOfAnotherUser_whenAdmin() {
        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Admin Edit,Desc,10,99.0,20.0,SHARED-REF\n";
        MockMultipartFile file = multipartFile(csv);

        when(securityUtils.isAdmin()).thenReturn(true);
        User anotherUser = new User();
        anotherUser.setId(99L);
        Product ownedByAnotherUser = new Product();
        ownedByAnotherUser.setCreatedBy(anotherUser);
        when(productRepository.findByReferenceCodeAndActiveTrue("SHARED-REF")).thenReturn(Optional.of(ownedByAnotherUser));
        when(productRepository.save(ownedByAnotherUser)).thenReturn(ownedByAnotherUser);
        when(productMapper.toResponse(ownedByAnotherUser)).thenAnswer(inv -> null);

        CsvImportResult result = csvImportService.importProducts(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        assertThat(ownedByAnotherUser.getLabel()).isEqualTo("Admin Edit");
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

    @Test
    void importProducts_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> csvImportService.importProducts(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Empty file");
    }

    @Test
    void importProducts_rejectsNonCsvExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "products.txt", "text/csv",
                "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n".getBytes());

        assertThatThrownBy(() -> csvImportService.importProducts(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only .csv files are accepted");
    }

    @Test
    void importProducts_rejectsDisallowedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "application/octet-stream",
                "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n".getBytes());

        assertThatThrownBy(() -> csvImportService.importProducts(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid content type");
    }

    @Test
    void importProducts_rejectsTooManyRows() {
        CsvImportProperties strictProperties = new CsvImportProperties(1, List.of("text/csv"));
        csvImportService = new CsvImportServiceImpl(productRepository, productMapper,
                new CsvImportColumns("label", "description", "stockQuantity", "unitPrice", "vatRate", "referenceCode"),
                strictProperties, securityUtils);

        String csv = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n" +
                     "Robinet,Desc,5,85.0,20.0,REF-001\n" +
                     "Tuyau,Desc,5,85.0,20.0,REF-002\n";
        MockMultipartFile file = multipartFile(csv);

        assertThatThrownBy(() -> csvImportService.importProducts(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Too many rows");
    }

    private MockMultipartFile multipartFile(String csv) {
        return new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
