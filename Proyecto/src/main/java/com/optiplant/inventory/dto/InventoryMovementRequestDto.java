package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.MovementType;
import com.optiplant.inventory.enumtype.ReferenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementRequestDto {

    @NotNull(message = "La sucursal es obligatoria")
    private Long branchId;

    @NotNull(message = "El producto es obligatorio")
    private Long productId;

    @NotNull(message = "El usuario responsable es obligatorio")
    private Long performedByUserId;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private MovementType movementType;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    private BigDecimal unitCost;

    @NotBlank(message = "La razón del movimiento es obligatoria")
    private String reason;

    private ReferenceType referenceType;
    private Long referenceId;
}