package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.SaleStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponseDto {

    private Long id;
    private Long branchId;
    private String branchName;
    private LocalDateTime saleDate;
    private String customerName;
    private SaleStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal total;
    private Long createdByUserId;
    private String createdByUserName;
    private List<SaleItemResponseDto> items;
}