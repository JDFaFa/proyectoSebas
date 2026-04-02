package com.optiplant.inventory.service;

import com.optiplant.inventory.dto.SaleRequestDto;
import com.optiplant.inventory.dto.SaleResponseDto;

import java.util.List;

public interface SaleService {

    List<SaleResponseDto> getAllSales();

    SaleResponseDto getSaleById(Long id);

    SaleResponseDto createSale(SaleRequestDto requestDto);

    SaleResponseDto cancelSale(Long saleId);
}