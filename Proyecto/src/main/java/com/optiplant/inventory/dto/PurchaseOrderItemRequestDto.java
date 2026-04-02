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
public class PurchaseOrderItemRequestDto {

    @NotNull(message = "El producto es obligatorio")
    private Long productId;

    @NotNull(message = "La unidad es obligatoria")
    private Long unitId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "La cantidad recibida no puede ser negativa")
    private BigDecimal receivedQuantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    private BigDecimal discountPercent;
}