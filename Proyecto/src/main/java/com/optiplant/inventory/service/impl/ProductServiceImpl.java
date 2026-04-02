package com.optiplant.inventory.service.impl;

import com.optiplant.inventory.dto.ProductRequestDto;
import com.optiplant.inventory.dto.ProductResponseDto;
import com.optiplant.inventory.entity.Product;
import com.optiplant.inventory.repository.ProductRepository;
import com.optiplant.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        return mapToResponseDto(product);
    }

    @Override
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        if (productRepository.existsBySku(requestDto.getSku())) {
            throw new RuntimeException("Ya existe un producto con el SKU: " + requestDto.getSku());
        }

        Product product = Product.builder()
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .category(requestDto.getCategory())
                .brand(requestDto.getBrand())
                .active(requestDto.getActive() != null ? requestDto.getActive() : true)
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    @Override
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        if (!product.getSku().equals(requestDto.getSku()) &&
                productRepository.existsBySku(requestDto.getSku())) {
            throw new RuntimeException("Ya existe otro producto con el SKU: " + requestDto.getSku());
        }

        product.setSku(requestDto.getSku());
        product.setName(requestDto.getName());
        product.setDescription(requestDto.getDescription());
        product.setCategory(requestDto.getCategory());
        product.setBrand(requestDto.getBrand());

        if (requestDto.getActive() != null) {
            product.setActive(requestDto.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        product.setActive(false);
        productRepository.save(product);
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}