package com.slim.kreadevis_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "professionals")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prof_seq_gen")
    @SequenceGenerator(name = "prof_seq_gen", sequenceName = "prof_seq", initialValue = 11, allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(nullable = false)
    private String streetNumber;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String city;

    private String company;

    private String vat;

    private String siren;

    @OneToOne
    @JoinColumn(unique = true)
    private User user;
}
