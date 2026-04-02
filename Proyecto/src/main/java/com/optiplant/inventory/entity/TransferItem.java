package com.optiplant.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transfer_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ProductUnit unit;

    @Column(name = "requested_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal requestedQuantity;

    @Column(name = "approved_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal approvedQuantity = BigDecimal.ZERO;

    @Column(name = "shipped_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippedQuantity = BigDecimal.ZERO;

    @Column(name = "received_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "missing_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal missingQuantity = BigDecimal.ZERO;

    @Column(length = 255)
    private String observation;
}