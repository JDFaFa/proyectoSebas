package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.PurchaseOrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDto {

    private Long id;
    private Long branchId;
    private String branchName;
    private Long supplierId;
    private String supplierName;
    private PurchaseOrderStatus status;
    private LocalDateTime purchaseDate;
    private LocalDateTime expectedDeliveryDate;
    private String paymentTerm;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal total;
    private Long createdByUserId;
    private String createdByUserName;
    private List<PurchaseOrderItemResponseDto> items;
}