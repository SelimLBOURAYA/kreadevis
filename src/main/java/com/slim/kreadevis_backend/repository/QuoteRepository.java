package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByClientId(Long clientId);

    Optional<Quote> findByReferenceCode(String referenceCode);
}
