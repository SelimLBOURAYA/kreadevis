package com.slim.kreadevis_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"client", "items", "createdBy"})
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quote_seq_gen")
    @SequenceGenerator(name = "quote_seq_gen", sequenceName = "quote_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Builder.Default
    @Column
    private LocalDate date = LocalDate.now();

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(name = "daily_sequence")
    private int dailySequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.DRAFT;

    @ManyToOne
    @JsonIgnoreProperties("quotes")
    private Client client;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL)
    @Builder.Default
    private List<QuoteItem> items = new ArrayList<>();
}
