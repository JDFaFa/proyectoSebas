package com.optiplant.inventory.service.impl;

import com.optiplant.inventory.dto.InventoryMovementRequestDto;
import com.optiplant.inventory.dto.InventoryMovementResponseDto;
import com.optiplant.inventory.dto.InventoryResponseDto;
import com.optiplant.inventory.entity.*;
import com.optiplant.inventory.enumtype.*;
import com.optiplant.inventory.repository.*;
import com.optiplant.inventory.service.InventoryService;
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
public class InventoryServiceImpl implements InventoryService {

    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final StockAlertRepository stockAlertRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getInventoryByBranch(Long branchId) {
        validateBranchExists(branchId);

        return branchInventoryRepository.findByBranchId(branchId)
                .stream()
                .map(this::mapToInventoryResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDto getInventoryByBranchAndProduct(Long branchId, Long productId) {
        BranchInventory inventory = branchInventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "No existe inventario para la sucursal " + branchId + " y el producto " + productId
                ));

        return mapToInventoryResponseDto(inventory);
    }

    @Override
    public InventoryMovementResponseDto registerEntry(InventoryMovementRequestDto requestDto) {
        validateEntryMovementType(requestDto.getMovementType());

        Branch branch = getBranchOrThrow(requestDto.getBranchId());
        Product product = getProductOrThrow(requestDto.getProductId());
        User user = getUserOrThrow(requestDto.getPerformedByUserId());

        BranchInventory inventory = getOrCreateInventory(branch, product);

        BigDecimal currentStock = safe(inventory.getStockQuantity());
        BigDecimal entryQuantity = requestDto.getQuantity();
        BigDecimal newStock = currentStock.add(entryQuantity);

        inventory.setStockQuantity(newStock);

        if (requestDto.getUnitCost() != null) {
            BigDecimal newAverageCost = calculateAverageCost(
                    currentStock,
                    safe(inventory.getAverageCost()),
                    entryQuantity,
                    requestDto.getUnitCost()
            );
            inventory.setAverageCost(newAverageCost);
        }

        BranchInventory savedInventory = branchInventoryRepository.save(inventory);

        BigDecimal movementUnitCost = requestDto.getUnitCost() != null
                ? requestDto.getUnitCost()
                : safe(savedInventory.getAverageCost());

        InventoryMovement movement = InventoryMovement.builder()
                .branch(branch)
                .product(product)
                .movementType(requestDto.getMovementType())
                .quantity(entryQuantity)
                .unitCost(movementUnitCost)
                .totalCost(entryQuantity.multiply(movementUnitCost).setScale(2, RoundingMode.HALF_UP))
                .reason(requestDto.getReason())
                .referenceType(requestDto.getReferenceType())
                .referenceId(requestDto.getReferenceId())
                .performedBy(user)
                .movementDate(LocalDateTime.now())
                .build();

        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        processStockAlerts(savedInventory);

        return mapToMovementResponseDto(savedMovement);
    }

    @Override
    public InventoryMovementResponseDto registerExit(InventoryMovementRequestDto requestDto) {
        validateExitMovementType(requestDto.getMovementType());

        Branch branch = getBranchOrThrow(requestDto.getBranchId());
        Product product = getProductOrThrow(requestDto.getProductId());
        User user = getUserOrThrow(requestDto.getPerformedByUserId());

        BranchInventory inventory = branchInventoryRepository.findByBranchIdAndProductId(branch.getId(), product.getId())
                .orElseThrow(() -> new RuntimeException("No existe inventario registrado para este producto en la sucursal"));

        BigDecimal currentStock = safe(inventory.getStockQuantity());
        BigDecimal exitQuantity = requestDto.getQuantity();

        if (currentStock.compareTo(exitQuantity) < 0) {
            throw new RuntimeException("Stock insuficiente para realizar la salida");
        }

        BigDecimal newStock = currentStock.subtract(exitQuantity);
        inventory.setStockQuantity(newStock);

        BranchInventory savedInventory = branchInventoryRepository.save(inventory);

        BigDecimal movementUnitCost = requestDto.getUnitCost() != null
                ? requestDto.getUnitCost()
                : safe(savedInventory.getAverageCost());

        InventoryMovement movement = InventoryMovement.builder()
                .branch(branch)
                .product(product)
                .movementType(requestDto.getMovementType())
                .quantity(exitQuantity)
                .unitCost(movementUnitCost)
                .totalCost(exitQuantity.multiply(movementUnitCost).setScale(2, RoundingMode.HALF_UP))
                .reason(requestDto.getReason())
                .referenceType(requestDto.getReferenceType())
                .referenceId(requestDto.getReferenceId())
                .performedBy(user)
                .movementDate(LocalDateTime.now())
                .build();

        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        processStockAlerts(savedInventory);

        return mapToMovementResponseDto(savedMovement);
    }

    private void validateBranchExists(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new RuntimeException("Sucursal no encontrada con id: " + branchId);
        }
    }

    private Branch getBranchOrThrow(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con id: " + branchId));
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + productId));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
    }

    private BranchInventory getOrCreateInventory(Branch branch, Product product) {
        return branchInventoryRepository.findByBranchIdAndProductId(branch.getId(), product.getId())
                .orElseGet(() -> BranchInventory.builder()
                        .branch(branch)
                        .product(product)
                        .stockQuantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .averageCost(BigDecimal.ZERO)
                        .minimumStock(BigDecimal.ZERO)
                        .build());
    }

    private void validateEntryMovementType(MovementType movementType) {
        if (movementType != MovementType.PURCHASE_IN
                && movementType != MovementType.ADJUSTMENT_IN
                && movementType != MovementType.RETURN_IN
                && movementType != MovementType.TRANSFER_IN) {
            throw new RuntimeException("Tipo de movimiento no válido para una entrada");
        }
    }

    private void validateExitMovementType(MovementType movementType) {
        if (movementType != MovementType.SALE_OUT
                && movementType != MovementType.ADJUSTMENT_OUT
                && movementType != MovementType.WASTE_OUT
                && movementType != MovementType.TRANSFER_OUT) {
            throw new RuntimeException("Tipo de movimiento no válido para una salida");
        }
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

    private void processStockAlerts(BranchInventory inventory) {
        BigDecimal stock = safe(inventory.getStockQuantity());
        BigDecimal minimum = safe(inventory.getMinimumStock());

        if (stock.compareTo(BigDecimal.ZERO) == 0) {
            createAlertIfNotExists(
                    inventory,
                    AlertType.OUT_OF_STOCK,
                    "El producto quedó agotado en la sucursal"
            );
            resolveAlertIfExists(inventory, AlertType.LOW_STOCK);
            return;
        }

        if (stock.compareTo(minimum) <= 0) {
            createAlertIfNotExists(
                    inventory,
                    AlertType.LOW_STOCK,
                    "El producto está en nivel mínimo o por debajo del stock mínimo"
            );
            resolveAlertIfExists(inventory, AlertType.OUT_OF_STOCK);
            return;
        }

        resolveAlertIfExists(inventory, AlertType.LOW_STOCK);
        resolveAlertIfExists(inventory, AlertType.OUT_OF_STOCK);
    }

    private void createAlertIfNotExists(BranchInventory inventory, AlertType alertType, String message) {
        stockAlertRepository.findByBranchIdAndProductIdAndAlertTypeAndStatus(
                inventory.getBranch().getId(),
                inventory.getProduct().getId(),
                alertType,
                AlertStatus.ACTIVE
        ).orElseGet(() -> stockAlertRepository.save(
                StockAlert.builder()
                        .branch(inventory.getBranch())
                        .product(inventory.getProduct())
                        .alertType(alertType)
                        .message(message)
                        .status(AlertStatus.ACTIVE)
                        .build()
        ));
    }

    private void resolveAlertIfExists(BranchInventory inventory, AlertType alertType) {
        stockAlertRepository.findByBranchIdAndProductIdAndAlertTypeAndStatus(
                inventory.getBranch().getId(),
                inventory.getProduct().getId(),
                alertType,
                AlertStatus.ACTIVE
        ).ifPresent(alert -> {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            stockAlertRepository.save(alert);
        });
    }

    private InventoryResponseDto mapToInventoryResponseDto(BranchInventory inventory) {
        BigDecimal stock = safe(inventory.getStockQuantity());
        BigDecimal reserved = safe(inventory.getReservedQuantity());
        BigDecimal available = stock.subtract(reserved);
        BigDecimal minimum = safe(inventory.getMinimumStock());

        return InventoryResponseDto.builder()
                .branchId(inventory.getBranch().getId())
                .branchName(inventory.getBranch().getName())
                .productId(inventory.getProduct().getId())
                .productSku(inventory.getProduct().getSku())
                .productName(inventory.getProduct().getName())
                .stockQuantity(stock)
                .reservedQuantity(reserved)
                .availableQuantity(available)
                .averageCost(safe(inventory.getAverageCost()))
                .minimumStock(minimum)
                .lowStock(stock.compareTo(minimum) <= 0)
                .build();
    }

    private InventoryMovementResponseDto mapToMovementResponseDto(InventoryMovement movement) {
        return InventoryMovementResponseDto.builder()
                .id(movement.getId())
                .branchId(movement.getBranch().getId())
                .productId(movement.getProduct().getId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .unitCost(movement.getUnitCost())
                .totalCost(movement.getTotalCost())
                .reason(movement.getReason())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .performedByUserId(movement.getPerformedBy().getId())
                .performedByName(movement.getPerformedBy().getFullName())
                .movementDate(movement.getMovementDate())
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}