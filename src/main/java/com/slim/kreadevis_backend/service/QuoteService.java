package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.entity.Quote;

public interface QuoteService {
    String generateReferenceCode(Quote quote);
}
