package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.TransferPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDto {

    @NotNull(message = "La sucursal solicitante es obligatoria")
    private Long requestingBranchId;

    @NotNull(message = "La sucursal origen es obligatoria")
    private Long sourceBranchId;

    @NotNull(message = "La sucursal destino es obligatoria")
    private Long destinationBranchId;

    @NotNull(message = "El usuario solicitante es obligatorio")
    private Long requestedByUserId;

    private TransferPriority priority;

    private String transportCompany;
    private String trackingNumber;
    private LocalDateTime estimatedArrivalDate;
    private String notes;

    @Valid
    @NotEmpty(message = "La transferencia debe tener al menos un item")
    private List<TransferItemRequestDto> items;
}