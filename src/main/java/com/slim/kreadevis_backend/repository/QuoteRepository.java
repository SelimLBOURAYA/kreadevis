package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByClientIdAndActiveTrue(Long clientId);

    List<Quote> findByClientIdAndActiveTrueAndCreatedById(Long clientId, Long ownerId);

    @Query("SELECT q FROM Quote q WHERE q.active = true "
            + "AND (:status IS NULL OR q.status = :status) "
            + "AND (:startDate IS NULL OR q.date >= :startDate) "
            + "AND (:endDate IS NULL OR q.date <= :endDate)")
    Page<Quote> findByFilters(@Param("status") QuoteStatus status,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               Pageable pageable);

    @Query("SELECT q FROM Quote q WHERE q.active = true AND q.createdBy.id = :ownerId "
            + "AND (:status IS NULL OR q.status = :status) "
            + "AND (:startDate IS NULL OR q.date >= :startDate) "
            + "AND (:endDate IS NULL OR q.date <= :endDate)")
    Page<Quote> findByFiltersForOwner(@Param("ownerId") Long ownerId,
                                       @Param("status") QuoteStatus status,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       Pageable pageable);

    Optional<Quote> findByReferenceCodeAndActiveTrue(String referenceCode);

    Optional<Quote> findByReferenceCodeAndActiveTrueAndCreatedById(String referenceCode, Long ownerId);

    Optional<Quote> findByIdAndActiveTrue(Long id);

    Optional<Quote> findByIdAndActiveTrueAndCreatedById(Long id, Long ownerId);

    Optional<Quote> findByIdAndCreatedById(Long id, Long ownerId);

    @Query("SELECT COALESCE(MAX(q.dailySequence), 0) FROM Quote q WHERE q.createdBy.id = :userId AND q.date = :date")
    int findMaxDailySequenceByUserAndDate(Long userId, LocalDate date);
}
