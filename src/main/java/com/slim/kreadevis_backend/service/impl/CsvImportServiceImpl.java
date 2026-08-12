package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.CsvImportColumns;
import com.slim.kreadevis_backend.dto.product.CsvImportResult;
import com.slim.kreadevis_backend.dto.product.CsvImportResult.RowError;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.service.CsvImportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CsvImportServiceImpl implements CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CsvImportColumns columns;
    private final SecurityUtils securityUtils;
    private final CSVFormat csvFormat;

    public CsvImportServiceImpl(ProductRepository productRepository,
                                ProductMapper productMapper,
                                CsvImportColumns columns,
                                SecurityUtils securityUtils) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.columns = columns;
        this.securityUtils = securityUtils;
        this.csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();
    }

    private record ProductRow(
            String label,
            String description,
            Long stockQuantity,
            BigDecimal unitPrice,
            BigDecimal vatRate,
            String referenceCode
    ) {}

    private sealed interface ParseResult {
        record Ok(ProductRow row) implements ParseResult {}
        record Err(RowError error) implements ParseResult {}
    }

    @Override
    @Transactional
    public CsvImportResult importProducts(MultipartFile file) {
        List<ProductResponse> imported = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = csvFormat.parse(reader)) {

            Set<String> headerNames = new HashSet<>(parser.getHeaderNames());
            validateColumns(headerNames);

            for (CSVRecord record : parser) {
                int lineNumber = (int) record.getRecordNumber() + 1;
                processRecord(record, lineNumber, imported, errors);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage(), e);
        }

        log.info("CSV import completed: {} imported, {} errors", imported.size(), errors.size());

        return new CsvImportResult(imported.size(), imported, errors, List.of());
    }

    private void validateColumns(Set<String> headerNames) {
        List<String> expected = List.of(
                columns.label(), columns.description(), columns.stockQuantity(),
                columns.unitPrice(), columns.vatRate(), columns.referenceCode());
        List<String> missing = expected.stream()
                .filter(col -> !headerNames.contains(col))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV missing expected columns: " + String.join(", ", missing)
                            + ". Header found: " + headerNames);
        }
    }

    private void processRecord(CSVRecord record, int lineNumber,
                               List<ProductResponse> imported,
                               List<RowError> errors) {
        switch (parseRow(record, lineNumber)) {
            case ParseResult.Err(RowError err) -> errors.add(err);
            case ParseResult.Ok(ProductRow row) -> persistOrUpdate(row, lineNumber, imported, errors);
        }
    }

    private ParseResult parseRow(CSVRecord record, int lineNumber) {
        String label = record.get(columns.label());
        if (label.isBlank()) {
            return new ParseResult.Err(new RowError(lineNumber, "Missing required field: label"));
        }

        Long stockQuantity = 0L;
        String sq = record.get(columns.stockQuantity());
        if (!sq.isBlank()) {
            try {
                stockQuantity = Long.parseLong(sq);
            } catch (NumberFormatException e) {
                return new ParseResult.Err(new RowError(lineNumber,
                        "Invalid stockQuantity: " + sq));
            }
        }

        BigDecimal unitPrice;
        try {
            unitPrice = new BigDecimal(record.get(columns.unitPrice()));
        } catch (NumberFormatException e) {
            return new ParseResult.Err(new RowError(lineNumber,
                    "Invalid unitPrice: " + record.get(columns.unitPrice())));
        }

        BigDecimal vatRate;
        try {
            vatRate = new BigDecimal(record.get(columns.vatRate()));
        } catch (NumberFormatException e) {
            return new ParseResult.Err(new RowError(lineNumber,
                    "Invalid vatRate: " + record.get(columns.vatRate())));
        }

        String description = blankToNull(record.get(columns.description()));
        String referenceCode = blankToNull(record.get(columns.referenceCode()));

        return new ParseResult.Ok(new ProductRow(
                label,
                description,
                stockQuantity,
                unitPrice,
                vatRate,
                referenceCode
        ));
    }

    private void persistOrUpdate(ProductRow row, int lineNumber,
                                 List<ProductResponse> imported,
                                 List<RowError> errors) {
        User currentUser = securityUtils.getCurrentUser();
        Product product;
        if (row.referenceCode() != null) {
            // Single unscoped read: it answers both "does it exist" and "is it mine",
            // which is what lets us tell an update apart from a conflict below.
            Optional<Product> existing = productRepository.findByReferenceCodeAndActiveTrue(row.referenceCode());
            if (existing.isPresent() && !isUsableBy(existing.get(), currentUser)) {
                errors.add(new RowError(lineNumber,
                        "Reference code already used: " + row.referenceCode()));
                return;
            }
            product = existing
                    .map(target -> {
                        target.setLabel(row.label());
                        target.setDescription(row.description());
                        target.setStockQuantity(row.stockQuantity());
                        target.setUnitPrice(row.unitPrice());
                        target.setVatRate(row.vatRate());
                        return target;
                    })
                    .orElseGet(() -> toEntity(row, currentUser));
        } else {
            product = toEntity(row, currentUser);
        }
        Product saved = productRepository.save(product);
        imported.add(productMapper.toResponse(saved));
    }

    /**
     * Admins can update any product by reference code; a regular user only their own.
     * {@code reference_code} is globally unique, so a code owned by someone else cannot
     * be re-created under the current user — the row is rejected rather than silently
     * overwriting another user's product.
     */
    private boolean isUsableBy(Product product, User currentUser) {
        return securityUtils.isAdmin() || product.getCreatedBy().getId().equals(currentUser.getId());
    }

    private Product toEntity(ProductRow row, User currentUser) {
        return Product.builder()
                .label(row.label())
                .description(row.description())
                .stockQuantity(row.stockQuantity())
                .unitPrice(row.unitPrice())
                .vatRate(row.vatRate())
                .referenceCode(row.referenceCode())
                .createdBy(currentUser)
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
