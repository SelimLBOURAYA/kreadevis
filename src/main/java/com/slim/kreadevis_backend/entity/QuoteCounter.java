package com.slim.kreadevis_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "quote_counters")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuoteCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "counter_seq_gen")
    @SequenceGenerator(name = "counter_seq_gen", sequenceName = "counter_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate = LocalDate.now();

    @Column(name = "quote_counter")
    private int quoteCounter;

    @Column(name = "invoice_counter")
    private int invoiceCounter;
}
