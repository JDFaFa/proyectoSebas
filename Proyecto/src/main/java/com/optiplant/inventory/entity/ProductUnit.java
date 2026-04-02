package com.optiplant.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "product_units",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_product_units_product_unit", columnNames = {"product_id", "unit_name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    @Column(name = "conversion_factor", nullable = false, precision = 12, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "is_base_unit", nullable = false)
    private Boolean baseUnit = false;
}