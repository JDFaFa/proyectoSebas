package com.optiplant.inventory.controller;

import com.optiplant.inventory.dto.PurchaseOrderRequestDto;
import com.optiplant.inventory.dto.PurchaseOrderResponseDto;
import com.optiplant.inventory.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponseDto>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseOrderService.createPurchaseOrder(requestDto));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponseDto> receivePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receivePurchaseOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponseDto> cancelPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.cancelPurchaseOrder(id));
    }
}