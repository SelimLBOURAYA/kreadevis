package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.product.CsvImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface CsvImportService {
    CsvImportResult importProducts(MultipartFile file);
}
