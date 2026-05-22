package com.slim.kreadevis_backend.dto.product;

import java.util.List;

public record CsvImportResult(
        int importedCount,
        List<ProductResponse> imported,
        List<RowError> errors,
        List<String> warnings
) {
    public record RowError(int lineNumber, String message) {}
}
