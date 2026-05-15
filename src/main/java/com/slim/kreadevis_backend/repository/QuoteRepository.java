package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByClientId(Long clientId);

    Optional<Quote> findByReferenceCode(String referenceCode);

    @Query("SELECT MAX(q.dailySequence) FROM Quote q WHERE q.date = :date")
    Optional<Integer> findMaxDailySequenceByDate(LocalDate date);
}
