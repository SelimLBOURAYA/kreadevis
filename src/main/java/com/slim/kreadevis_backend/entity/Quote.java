package com.slim.kreadevis_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "total_price_ht")
    private BigDecimal totalPriceHt;

    @Column(name = "total_vat")
    private BigDecimal totalVat;

    @Column(name = "total_price_ttc")
    private BigDecimal totalPriceTtc;

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

    @ManyToOne(optional = false)
    private Client client;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "sent_to")
    private String sentTo;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL)
    @Builder.Default
    private List<QuoteItem> items = new ArrayList<>();

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public void recomputeTotals() {
        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        for (QuoteItem item : this.items) {
            if (!item.isActive() || item.getTotalPrice() == null) continue;
            totalHt = totalHt.add(item.getTotalPrice());
            BigDecimal rate = item.getVatRate() != null ? item.getVatRate() : BigDecimal.ZERO;
            BigDecimal vatForLine = item.getTotalPrice()
                    .multiply(rate)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            totalVat = totalVat.add(vatForLine);
        }
        this.totalPriceHt = totalHt;
        this.totalVat = totalVat;
        this.totalPriceTtc = totalHt.add(totalVat);
    }
}
