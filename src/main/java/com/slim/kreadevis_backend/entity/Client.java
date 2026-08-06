package com.slim.kreadevis_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clients")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"address", "quotes", "createdBy"})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq_gen")
    @SequenceGenerator(name = "client_seq_gen", sequenceName = "client_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String company;

    private String siret;

    private String siren;

    @Column(name = "vat_code")
    private String vatCode;

    @ManyToOne(optional = false)
    private Address address;

    @Column(name = "phone")
    private String phone;

    @Column(length = 254, unique = true)
    private String email;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    @OneToMany(mappedBy = "client")
    private Set<Quote> quotes = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
