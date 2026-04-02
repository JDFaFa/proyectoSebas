package com.optiplant.inventory.controller;

import com.optiplant.inventory.dto.InventoryMovementRequestDto;
import com.optiplant.inventory.dto.InventoryMovementResponseDto;
import com.optiplant.inventory.dto.InventoryResponseDto;
import com.optiplant.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<InventoryResponseDto>> getInventoryByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(inventoryService.getInventoryByBranch(branchId));
    }

    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<InventoryResponseDto> getInventoryByBranchAndProduct(
            @PathVariable Long branchId,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(inventoryService.getInventoryByBranchAndProduct(branchId, productId));
    }

    @PostMapping("/entry")
    public ResponseEntity<InventoryMovementResponseDto> registerEntry(
            @Valid @RequestBody InventoryMovementRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.registerEntry(requestDto));
    }

    @PostMapping("/exit")
    public ResponseEntity<InventoryMovementResponseDto> registerExit(
            @Valid @RequestBody InventoryMovementRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.registerExit(requestDto));
    }
}