package com.optiplant.inventory.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private String brand;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}