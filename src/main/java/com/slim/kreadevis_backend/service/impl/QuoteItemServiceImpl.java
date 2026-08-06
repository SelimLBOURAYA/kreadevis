package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.quote.QuoteItemRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteItemResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import com.slim.kreadevis_backend.mapper.QuoteMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.repository.QuoteItemRepository;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.service.QuoteItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuoteItemServiceImpl implements QuoteItemService {

    private static final Set<QuoteStatus> LOCKED_STATUSES = Set.of(QuoteStatus.FINALIZED, QuoteStatus.CANCELLED);

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final ProductRepository productRepository;
    private final QuoteMapper quoteMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public QuoteItemResponse addItem(Long quoteId, QuoteItemRequest request) {
        Quote quote = loadModifiableQuote(quoteId);
        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        QuoteItem item = QuoteItem.builder()
                .quote(quote)
                .product(product)
                .quantity(request.quantity())
                .unitPrice(product.getUnitPrice())
                .vatRate(product.getVatRate())
                .totalPrice(product.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .build();

        QuoteItem saved = quoteItemRepository.save(item);
        quote.getItems().add(saved);
        quote.recomputeTotals();
        quoteRepository.save(quote);

        return quoteMapper.toItemResponse(saved);
    }

    @Override
    @Transactional
    public QuoteItemResponse updateItem(Long quoteId, Long itemId, QuoteItemRequest request) {
        Quote quote = loadModifiableQuote(quoteId);
        QuoteItem item = quoteItemRepository.findByIdAndActiveTrue(itemId)
                .orElseThrow(() -> new EntityNotFoundException("QuoteItem not found: " + itemId));
        if (!item.getQuote().getId().equals(quoteId)) {
            throw new EntityNotFoundException("QuoteItem " + itemId + " does not belong to quote " + quoteId);
        }
        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        item.setProduct(product);
        item.setQuantity(request.quantity());
        item.setUnitPrice(product.getUnitPrice());
        item.setVatRate(product.getVatRate());
        item.setTotalPrice(product.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())));

        QuoteItem saved = quoteItemRepository.save(item);
        quote.recomputeTotals();
        quoteRepository.save(quote);

        return quoteMapper.toItemResponse(saved);
    }

    @Override
    @Transactional
    public void deleteItem(Long quoteId, Long itemId) {
        Quote quote = loadModifiableQuote(quoteId);
        QuoteItem item = quoteItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("QuoteItem not found: " + itemId));
        if (!item.getQuote().getId().equals(quoteId)) {
            throw new EntityNotFoundException("QuoteItem " + itemId + " does not belong to quote " + quoteId);
        }
        item.setActive(false);
        quoteItemRepository.save(item);

        quote.recomputeTotals();
        quoteRepository.save(quote);
    }

    private Quote loadModifiableQuote(Long quoteId) {
        Quote quote = securityUtils.isAdmin()
                ? quoteRepository.findByIdAndActiveTrue(quoteId)
                        .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId))
                : quoteRepository.findByIdAndActiveTrueAndCreatedById(quoteId, securityUtils.getCurrentUser().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));
        if (LOCKED_STATUSES.contains(quote.getStatus())) {
            throw new IllegalStateException(
                    "Cannot modify items of a quote with status " + quote.getStatus());
        }
        return quote;
    }

}
