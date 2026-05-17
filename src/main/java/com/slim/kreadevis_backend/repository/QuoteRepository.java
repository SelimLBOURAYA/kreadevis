package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Quote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByClientId(Long clientId);

    @Query("SELECT q FROM Quote q WHERE (:startDate IS NULL OR q.date >= :startDate) AND (:endDate IS NULL OR q.date <= :endDate)")
    List<Quote> findByDateRange(LocalDate startDate, LocalDate endDate);

    Optional<Quote> findByReferenceCode(String referenceCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT MAX(q.dailySequence) FROM Quote q WHERE q.date = :date")
    Optional<Integer> findMaxDailySequenceByDate(LocalDate date);
}
