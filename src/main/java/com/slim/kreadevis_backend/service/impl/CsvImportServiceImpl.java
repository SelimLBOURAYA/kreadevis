package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.product.CsvImportResult;
import com.slim.kreadevis_backend.dto.product.CsvImportResult.RowError;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.mapper.ProductMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvImportServiceImpl implements CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportServiceImpl.class);

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader("label", "description", "stockQuantity", "unitPrice", "vatRate", "referenceCode")
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private record ProductRow(
            String label,
            String description,
            Long stockQuantity,
            float unitPrice,
            float vatRate,
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
        List<String> warnings = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSV_FORMAT.parse(reader);
            for (CSVRecord record : records) {
                int lineNumber = (int) record.getRecordNumber() + 1;
                processRecord(record, lineNumber, imported, errors, warnings);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage(), e);
        }

        log.info("CSV import completed: {} imported, {} errors, {} warnings",
                imported.size(), errors.size(), warnings.size());

        return new CsvImportResult(imported.size(), imported, errors, warnings);
    }

    private void processRecord(CSVRecord record, int lineNumber,
                               List<ProductResponse> imported,
                               List<RowError> errors,
                               List<String> warnings) {
        switch (parseRow(record, lineNumber)) {
            case ParseResult.Err(RowError err) -> errors.add(err);
            case ParseResult.Ok(ProductRow row) -> persistIfNew(row, lineNumber, imported, warnings);
        }
    }

    private ParseResult parseRow(CSVRecord record, int lineNumber) {
        String label = record.get("label");
        if (label.isBlank()) {
            return new ParseResult.Err(new RowError(lineNumber, "Missing required field: label"));
        }

        Long stockQuantity;
        try {
            stockQuantity = Long.parseLong(record.get("stockQuantity"));
        } catch (NumberFormatException e) {
            return new ParseResult.Err(new RowError(lineNumber, "Invalid stockQuantity: " + record.get("stockQuantity")));
        }

        float unitPrice;
        try {
            unitPrice = Float.parseFloat(record.get("unitPrice"));
        } catch (NumberFormatException e) {
            return new ParseResult.Err(new RowError(lineNumber, "Invalid unitPrice: " + record.get("unitPrice")));
        }

        float vatRate;
        try {
            vatRate = Float.parseFloat(record.get("vatRate"));
        } catch (NumberFormatException e) {
            return new ParseResult.Err(new RowError(lineNumber, "Invalid vatRate: " + record.get("vatRate")));
        }

        return new ParseResult.Ok(new ProductRow(
                label,
                blankToNull(record.get("description")),
                stockQuantity,
                unitPrice,
                vatRate,
                blankToNull(record.get("referenceCode"))
        ));
    }

    private void persistIfNew(ProductRow row, int lineNumber,
                              List<ProductResponse> imported, List<String> warnings) {
        if (row.referenceCode() != null
                && productRepository.existsByReferenceCodeAndActiveTrue(row.referenceCode())) {
            warnings.add("Line " + lineNumber + ": referenceCode '" + row.referenceCode() + "' already exists, skipped");
            return;
        }
        Product saved = productRepository.save(toEntity(row));
        imported.add(productMapper.toResponse(saved));
    }

    private Product toEntity(ProductRow row) {
        return Product.builder()
                .label(row.label())
                .description(row.description())
                .stockQuantity(row.stockQuantity())
                .unitPrice(row.unitPrice())
                .vatRate(row.vatRate())
                .referenceCode(row.referenceCode())
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
