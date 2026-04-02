package com.optiplant.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRequestDto {

    @NotNull(message = "La sucursal es obligatoria")
    private Long branchId;

    @NotNull(message = "El usuario creador es obligatorio")
    private Long createdByUserId;

    @Size(max = 150, message = "El nombre del cliente no puede superar 150 caracteres")
    private String customerName;

    @Valid
    @NotEmpty(message = "La venta debe tener al menos un item")
    private List<SaleItemRequestDto> items;
}