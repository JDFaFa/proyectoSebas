package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.MovementType;
import com.optiplant.inventory.enumtype.ReferenceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementResponseDto {

    private Long id;
    private Long branchId;
    private Long productId;
    private MovementType movementType;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String reason;
    private ReferenceType referenceType;
    private Long referenceId;
    private Long performedByUserId;
    private String performedByName;
    private LocalDateTime movementDate;
}