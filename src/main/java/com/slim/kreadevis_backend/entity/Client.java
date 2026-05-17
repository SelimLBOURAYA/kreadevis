package com.slim.kreadevis_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clients")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"address", "quotes"})
@SQLRestriction("deleted = false")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq_gen")
    @SequenceGenerator(name = "client_seq_gen", sequenceName = "client_seq", initialValue = 100001, allocationSize = 1)
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
    @JsonIgnoreProperties("clients")
    private Address address;

    @Column(name = "phone")
    private String phone;

    @Column(length = 254, unique = true)
    private String email;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;

    @OneToMany(mappedBy = "client")
    @JsonIgnoreProperties("client")
    private Set<Quote> quotes = new HashSet<>();
}
