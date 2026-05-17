package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.quote.QuoteItemRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteItemResponse;

public interface QuoteItemService {
    QuoteItemResponse addItem(Long quoteId, QuoteItemRequest request);
    QuoteItemResponse updateItem(Long quoteId, Long itemId, QuoteItemRequest request);
    void deleteItem(Long quoteId, Long itemId);
}
