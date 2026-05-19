package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {

    List<QuoteItem> findAllByActiveTrue();

    Optional<QuoteItem> findByIdAndActiveTrue(Long id);
}
