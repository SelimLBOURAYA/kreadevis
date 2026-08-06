package com.slim.kreadevis_backend.service;

public interface PdfService {
    byte[] generateQuotePdf(Long quoteId);
}
