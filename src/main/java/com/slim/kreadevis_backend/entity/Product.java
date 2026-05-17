package com.slim.kreadevis_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "supplier")
@SQLRestriction("deleted = false")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prod_seq_gen")
    @SequenceGenerator(name = "prod_seq_gen", sequenceName = "prod_seq", initialValue = 101, allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String label;

    private String description;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long stockQuantity;

    @Column(name = "unit_price")
    private float unitPrice;

    @Column(name = "vat_rate")
    private float vatRate;

    @Column(unique = true, name = "reference_code")
    private String referenceCode;

    @ManyToOne
    private Professional supplier;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;
}
