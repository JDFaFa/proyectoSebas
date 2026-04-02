package com.optiplant.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 50, message = "El SKU no puede superar los 50 caracteres")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String name;

    private String description;

    @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
    private String category;

    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String brand;

    private Boolean active;
}