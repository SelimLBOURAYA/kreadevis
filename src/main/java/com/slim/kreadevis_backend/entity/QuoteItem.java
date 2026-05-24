package com.slim.kreadevis_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "quote_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"product", "quote"})
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quote_item_seq_gen")
    @SequenceGenerator(name = "quote_item_seq_gen", sequenceName = "quote_item_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @ManyToOne
    @JsonIgnoreProperties("items")
    private Product product;

    @ManyToOne
    @JsonIgnoreProperties("items")
    private Quote quote;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;
}
