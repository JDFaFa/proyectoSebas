package com.optiplant.inventory.service;

import com.optiplant.inventory.dto.InventoryMovementRequestDto;
import com.optiplant.inventory.dto.InventoryMovementResponseDto;
import com.optiplant.inventory.dto.InventoryResponseDto;

import java.util.List;

public interface InventoryService {

    List<InventoryResponseDto> getInventoryByBranch(Long branchId);

    InventoryResponseDto getInventoryByBranchAndProduct(Long branchId, Long productId);

    InventoryMovementResponseDto registerEntry(InventoryMovementRequestDto requestDto);

    InventoryMovementResponseDto registerExit(InventoryMovementRequestDto requestDto);
}