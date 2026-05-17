package com.slim.kreadevis_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "quote_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"product", "quote"})
@SQLRestriction("deleted = false")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quote_item_seq_gen")
    @SequenceGenerator(name = "quote_item_seq_gen", sequenceName = "quote_item_seq", initialValue = 10001, allocationSize = 1)
    private Long id;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "unit_price")
    private float unitPrice;

    @Column(name = "total_price")
    private float totalPrice;

    @ManyToOne
    @JsonIgnoreProperties("items")
    private Product product;

    @ManyToOne
    @JsonIgnoreProperties("items")
    private Quote quote;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;
}
