package com.optiplant.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferItemRequestDto {

    @NotNull(message = "El producto es obligatorio")
    private Long productId;

    @NotNull(message = "La unidad es obligatoria")
    private Long unitId;

    @NotNull(message = "La cantidad solicitada es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad solicitada debe ser mayor que cero")
    private BigDecimal requestedQuantity;
}