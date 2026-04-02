package com.optiplant.inventory.service.impl;

import com.optiplant.inventory.dto.*;
import com.optiplant.inventory.entity.*;
import com.optiplant.inventory.enumtype.MovementType;
import com.optiplant.inventory.enumtype.PurchaseOrderStatus;
import com.optiplant.inventory.enumtype.ReferenceType;
import com.optiplant.inventory.repository.*;
import com.optiplant.inventory.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final BranchRepository branchRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponseDto> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(id);
        return mapToResponseDto(purchaseOrder);
    }

    @Override
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        Branch branch = branchRepository.findById(requestDto.getBranchId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con id: " + requestDto.getBranchId()));

        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + requestDto.getSupplierId()));

        User createdBy = userRepository.findById(requestDto.getCreatedByUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + requestDto.getCreatedByUserId()));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .branch(branch)
                .supplier(supplier)
                .status(PurchaseOrderStatus.DRAFT)
                .expectedDeliveryDate(requestDto.getExpectedDeliveryDate())
                .paymentTerm(requestDto.getPaymentTerm())
                .subtotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .createdBy(createdBy)
                .build();

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;

        for (PurchaseOrderItemRequestDto itemDto : requestDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + itemDto.getProductId()));

            ProductUnit unit = productUnitRepository.findById(itemDto.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unidad no encontrada con id: " + itemDto.getUnitId()));

            BigDecimal quantity = safe(itemDto.getQuantity());
            BigDecimal unitPrice = safe(itemDto.getUnitPrice());
            BigDecimal discountPercent = safe(itemDto.getDiscountPercent());
            BigDecimal receivedQuantity = safe(itemDto.getReceivedQuantity());

            BigDecimal grossLine = quantity.multiply(unitPrice);
            BigDecimal lineDiscount = grossLine
                    .multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = grossLine.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(savedOrder)
                    .product(product)
                    .unit(unit)
                    .quantity(quantity)
                    .receivedQuantity(receivedQuantity)
                    .unitPrice(unitPrice)
                    .discountPercent(discountPercent)
                    .lineTotal(lineTotal)
                    .build();

            purchaseOrderItemRepository.save(item);

            subtotal = subtotal.add(grossLine);
            discountTotal = discountTotal.add(lineDiscount);
        }

        savedOrder.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        savedOrder.setDiscountTotal(discountTotal.setScale(2, RoundingMode.HALF_UP));
        savedOrder.setTotal(subtotal.subtract(discountTotal).setScale(2, RoundingMode.HALF_UP));

        if (savedOrder.getStatus() == null) {
            savedOrder.setStatus(PurchaseOrderStatus.DRAFT);
        }

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(savedOrder);
        return mapToResponseDto(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDto receivePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new RuntimeException("No se puede recibir una orden de compra cancelada");
        }

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrderId);

        if (items.isEmpty()) {
            throw new RuntimeException("La orden de compra no tiene items");
        }

        boolean allComplete = true;

        for (PurchaseOrderItem item : items) {
            BigDecimal quantity = safe(item.getQuantity());
            BigDecimal receivedQuantity = safe(item.getReceivedQuantity());

            if (receivedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (receivedQuantity.compareTo(quantity) < 0) {
                allComplete = false;
            }

            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductId(
                            purchaseOrder.getBranch().getId(),
                            item.getProduct().getId()
                    )
                    .orElseGet(() -> BranchInventory.builder()
                            .branch(purchaseOrder.getBranch())
                            .product(item.getProduct())
                            .stockQuantity(BigDecimal.ZERO)
                            .reservedQuantity(BigDecimal.ZERO)
                            .averageCost(BigDecimal.ZERO)
                            .minimumStock(BigDecimal.ZERO)
                            .build());

            BigDecimal currentStock = safe(inventory.getStockQuantity());
            BigDecimal currentAverageCost = safe(inventory.getAverageCost());
            BigDecimal entryUnitCost = safe(item.getUnitPrice());

            BigDecimal newAverageCost = calculateAverageCost(
                    currentStock,
                    currentAverageCost,
                    receivedQuantity,
                    entryUnitCost
            );

            inventory.setStockQuantity(currentStock.add(receivedQuantity));
            inventory.setAverageCost(newAverageCost);

            BranchInventory savedInventory = branchInventoryRepository.save(inventory);

            InventoryMovement movement = InventoryMovement.builder()
                    .branch(savedInventory.getBranch())
                    .product(savedInventory.getProduct())
                    .movementType(MovementType.PURCHASE_IN)
                    .quantity(receivedQuantity)
                    .unitCost(entryUnitCost)
                    .totalCost(receivedQuantity.multiply(entryUnitCost).setScale(2, RoundingMode.HALF_UP))
                    .reason("Recepción de orden de compra #" + purchaseOrder.getId())
                    .referenceType(ReferenceType.PURCHASE_ORDER)
                    .referenceId(purchaseOrder.getId())
                    .performedBy(purchaseOrder.getCreatedBy())
                    .movementDate(LocalDateTime.now())
                    .build();

            inventoryMovementRepository.save(movement);
        }

        purchaseOrder.setStatus(allComplete
                ? PurchaseOrderStatus.RECEIVED_COMPLETE
                : PurchaseOrderStatus.RECEIVED_PARTIAL);

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        return mapToResponseDto(savedOrder);
    }

    @Override
    public PurchaseOrderResponseDto cancelPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED_COMPLETE
                || purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED_PARTIAL) {
            throw new RuntimeException("No se puede cancelar una orden de compra ya recibida");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        return mapToResponseDto(savedOrder);
    }

    private PurchaseOrder getPurchaseOrderOrThrow(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada con id: " + id));
    }

    private PurchaseOrderResponseDto mapToResponseDto(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderItemResponseDto> items = purchaseOrderItemRepository
                .findByPurchaseOrderId(purchaseOrder.getId())
                .stream()
                .map(item -> PurchaseOrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .unitId(item.getUnit().getId())
                        .unitName(item.getUnit().getUnitName())
                        .quantity(item.getQuantity())
                        .receivedQuantity(item.getReceivedQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountPercent(item.getDiscountPercent())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return PurchaseOrderResponseDto.builder()
                .id(purchaseOrder.getId())
                .branchId(purchaseOrder.getBranch().getId())
                .branchName(purchaseOrder.getBranch().getName())
                .supplierId(purchaseOrder.getSupplier().getId())
                .supplierName(purchaseOrder.getSupplier().getName())
                .status(purchaseOrder.getStatus())
                .purchaseDate(purchaseOrder.getPurchaseDate())
                .expectedDeliveryDate(purchaseOrder.getExpectedDeliveryDate())
                .paymentTerm(purchaseOrder.getPaymentTerm())
                .subtotal(purchaseOrder.getSubtotal())
                .discountTotal(purchaseOrder.getDiscountTotal())
                .total(purchaseOrder.getTotal())
                .createdByUserId(purchaseOrder.getCreatedBy().getId())
                .createdByUserName(purchaseOrder.getCreatedBy().getFullName())
                .items(items)
                .build();
    }

    private BigDecimal calculateAverageCost(
            BigDecimal currentStock,
            BigDecimal currentAverageCost,
            BigDecimal entryQuantity,
            BigDecimal entryUnitCost
    ) {
        BigDecimal currentValue = currentStock.multiply(currentAverageCost);
        BigDecimal entryValue = entryQuantity.multiply(entryUnitCost);
        BigDecimal totalStock = currentStock.add(entryQuantity);

        if (totalStock.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentValue.add(entryValue)
                .divide(totalStock, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}