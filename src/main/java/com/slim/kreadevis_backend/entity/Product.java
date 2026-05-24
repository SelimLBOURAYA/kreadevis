package com.slim.kreadevis_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "supplier")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prod_seq_gen")
    @SequenceGenerator(name = "prod_seq_gen", sequenceName = "prod_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String label;

    private String description;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long stockQuantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "vat_rate")
    private BigDecimal vatRate;

    @Column(unique = true, name = "reference_code")
    private String referenceCode;

    @ManyToOne
    private Professional supplier;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;
}
