package com.optiplant.inventory.service;

import com.optiplant.inventory.dto.PurchaseOrderResponseDto;
import com.optiplant.inventory.dto.PurchaseOrderRequestDto;

import java.util.List;

public interface PurchaseOrderService {

    List<PurchaseOrderResponseDto> getAllPurchaseOrders();

    PurchaseOrderResponseDto getPurchaseOrderById(Long id);

    PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto requestDto);

    PurchaseOrderResponseDto receivePurchaseOrder(Long purchaseOrderId);

    PurchaseOrderResponseDto cancelPurchaseOrder(Long purchaseOrderId);
}