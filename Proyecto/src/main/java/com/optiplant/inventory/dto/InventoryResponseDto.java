package com.optiplant.inventory.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponseDto {

    private Long branchId;
    private String branchName;

    private Long productId;
    private String productSku;
    private String productName;

    private BigDecimal stockQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal averageCost;
    private BigDecimal minimumStock;

    private Boolean lowStock;
}