package com.optiplant.inventory.service;

import com.optiplant.inventory.dto.ProductRequestDto;
import com.optiplant.inventory.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {

    List<ProductResponseDto> getAllProducts();

    ProductResponseDto getProductById(Long id);

    ProductResponseDto createProduct(ProductRequestDto requestDto);

    ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto);

    void deleteProduct(Long id);
}