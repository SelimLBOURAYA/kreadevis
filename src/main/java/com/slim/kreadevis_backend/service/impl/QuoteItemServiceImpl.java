package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.quote.QuoteItemRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteItemResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;
import com.slim.kreadevis_backend.mapper.QuoteMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.repository.QuoteItemRepository;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.service.QuoteItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuoteItemServiceImpl implements QuoteItemService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final ProductRepository productRepository;
    private final QuoteMapper quoteMapper;

    @Override
    @Transactional
    public QuoteItemResponse addItem(Long quoteId, QuoteItemRequest request) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        QuoteItem item = QuoteItem.builder()
                .quote(quote)
                .product(product)
                .quantity(request.quantity())
                .unitPrice(product.getUnitPrice())
                .totalPrice(product.getUnitPrice() * request.quantity())
                .build();

        return quoteMapper.toItemResponse(quoteItemRepository.save(item));
    }

    @Override
    @Transactional
    public QuoteItemResponse updateItem(Long quoteId, Long itemId, QuoteItemRequest request) {
        QuoteItem item = quoteItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("QuoteItem not found: " + itemId));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        item.setProduct(product);
        item.setQuantity(request.quantity());
        item.setUnitPrice(product.getUnitPrice());
        item.setTotalPrice(product.getUnitPrice() * request.quantity());

        return quoteMapper.toItemResponse(quoteItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long quoteId, Long itemId) {
        QuoteItem item = quoteItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("QuoteItem not found: " + itemId));
        item.setDeleted(true);
        quoteItemRepository.save(item);
    }
}
