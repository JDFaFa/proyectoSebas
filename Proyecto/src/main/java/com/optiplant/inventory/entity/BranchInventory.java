package com.optiplant.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "branch_inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_branch_inventory_branch_product", columnNames = {"branch_id", "product_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(name = "minimum_stock", nullable = false, precision = 14, scale = 2)
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}