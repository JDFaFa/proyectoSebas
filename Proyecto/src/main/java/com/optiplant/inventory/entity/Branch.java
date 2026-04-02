package com.optiplant.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private Boolean active = true;
}