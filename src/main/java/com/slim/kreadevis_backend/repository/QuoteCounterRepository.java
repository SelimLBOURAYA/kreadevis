package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.QuoteCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuoteCounterRepository extends JpaRepository<QuoteCounter, Long> {

    Optional<QuoteCounter> findFirstByOrderByIdDesc();
}
