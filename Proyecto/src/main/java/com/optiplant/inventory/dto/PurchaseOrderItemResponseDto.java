package com.optiplant.inventory.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private Long unitId;
    private String unitName;
    private BigDecimal quantity;
    private BigDecimal receivedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal lineTotal;
}
