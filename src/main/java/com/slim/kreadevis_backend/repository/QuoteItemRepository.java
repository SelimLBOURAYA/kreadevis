package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {

    List<QuoteItem> findByQuoteId(Long quoteId);

    @Query("SELECT qi FROM QuoteItem qi WHERE qi.quote.client.id = ?1 AND qi.quote.id = (SELECT max(q.id) FROM Quote q) AND qi.quote.finished = false")
    List<QuoteItem> findLastBookingByClientId(Long clientId);
}
