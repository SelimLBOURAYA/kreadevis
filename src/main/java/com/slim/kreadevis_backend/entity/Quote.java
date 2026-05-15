package com.slim.kreadevis_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "quotes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"client", "counter"})
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quote_seq_gen")
    @SequenceGenerator(name = "quote_seq_gen", sequenceName = "quote_seq", initialValue = 101, allocationSize = 1)
    private Long id;

    @Column(name = "total_price")
    private float totalPrice;

    @Column
    private LocalDate date = LocalDate.now();

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean finished;

    @ManyToOne
    @JsonIgnoreProperties("quotes")
    private Client client;

    @OneToOne
    @JoinColumn(name = "counter_id")
    private QuoteCounter counter;
}
