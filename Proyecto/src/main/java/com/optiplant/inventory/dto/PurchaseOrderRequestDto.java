package com.optiplant.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequestDto {

    @NotNull(message = "La sucursal es obligatoria")
    private Long branchId;

    @NotNull(message = "El proveedor es obligatorio")
    private Long supplierId;

    @NotNull(message = "El usuario creador es obligatorio")
    private Long createdByUserId;

    private LocalDateTime expectedDeliveryDate;

    @Size(max = 100, message = "El plazo de pago no puede superar 100 caracteres")
    private String paymentTerm;

    @Valid
    @NotEmpty(message = "La orden de compra debe tener al menos un item")
    private List<PurchaseOrderItemRequestDto> items;
}