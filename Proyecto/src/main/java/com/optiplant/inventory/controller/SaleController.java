package com.optiplant.inventory.controller;

import com.optiplant.inventory.dto.SaleRequestDto;
import com.optiplant.inventory.dto.SaleResponseDto;
import com.optiplant.inventory.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDto> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @PostMapping
    public ResponseEntity<SaleResponseDto> createSale(@Valid @RequestBody SaleRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createSale(requestDto));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleResponseDto> cancelSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.cancelSale(id));
    }
}