package com.optiplant.inventory.service.impl;

import com.optiplant.inventory.dto.*;
import com.optiplant.inventory.entity.*;
import com.optiplant.inventory.enumtype.MovementType;
import com.optiplant.inventory.enumtype.ReferenceType;
import com.optiplant.inventory.enumtype.SaleStatus;
import com.optiplant.inventory.repository.*;
import com.optiplant.inventory.service.SaleService;
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
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getAllSales() {
        return saleRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getSaleById(Long id) {
        Sale sale = getSaleOrThrow(id);
        return mapToResponseDto(sale);
    }

    @Override
    public SaleResponseDto createSale(SaleRequestDto requestDto) {
        Branch branch = branchRepository.findById(requestDto.getBranchId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con id: " + requestDto.getBranchId()));

        User createdBy = userRepository.findById(requestDto.getCreatedByUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + requestDto.getCreatedByUserId()));

        Sale sale = Sale.builder()
                .branch(branch)
                .customerName(requestDto.getCustomerName())
                .status(SaleStatus.COMPLETED)
                .subtotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .createdBy(createdBy)
                .build();

        Sale savedSale = saleRepository.save(sale);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;

        for (SaleItemRequestDto itemDto : requestDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + itemDto.getProductId()));

            ProductUnit unit = productUnitRepository.findById(itemDto.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unidad no encontrada con id: " + itemDto.getUnitId()));

            if (!unit.getProduct().getId().equals(product.getId())) {
                throw new RuntimeException("La unidad no corresponde al producto seleccionado");
            }

            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductId(branch.getId(), product.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "No existe inventario para el producto " + product.getName() + " en la sucursal"
                    ));

            BigDecimal quantity = safe(itemDto.getQuantity());
            BigDecimal stock = safe(inventory.getStockQuantity());

            if (stock.compareTo(quantity) < 0) {
                throw new RuntimeException("Stock insuficiente para el producto: " + product.getName());
            }

            BigDecimal unitPrice = safe(itemDto.getUnitPrice());
            BigDecimal discountPercent = safe(itemDto.getDiscountPercent());

            BigDecimal grossLine = quantity.multiply(unitPrice);
            BigDecimal lineDiscount = grossLine
                    .multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = grossLine.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP);

            SaleItem saleItem = SaleItem.builder()
                    .sale(savedSale)
                    .product(product)
                    .unit(unit)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountPercent(discountPercent)
                    .lineTotal(lineTotal)
                    .build();

            saleItemRepository.save(saleItem);

            inventory.setStockQuantity(stock.subtract(quantity));
            branchInventoryRepository.save(inventory);

            InventoryMovement movement = InventoryMovement.builder()
                    .branch(branch)
                    .product(product)
                    .movementType(MovementType.SALE_OUT)
                    .quantity(quantity)
                    .unitCost(safe(inventory.getAverageCost()))
                    .totalCost(quantity.multiply(safe(inventory.getAverageCost())).setScale(2, RoundingMode.HALF_UP))
                    .reason("Venta #" + savedSale.getId())
                    .referenceType(ReferenceType.SALE)
                    .referenceId(savedSale.getId())
                    .performedBy(createdBy)
                    .movementDate(LocalDateTime.now())
                    .build();

            inventoryMovementRepository.save(movement);

            subtotal = subtotal.add(grossLine);
            discountTotal = discountTotal.add(lineDiscount);
        }

        savedSale.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        savedSale.setDiscountTotal(discountTotal.setScale(2, RoundingMode.HALF_UP));
        savedSale.setTotal(subtotal.subtract(discountTotal).setScale(2, RoundingMode.HALF_UP));

        Sale updatedSale = saleRepository.save(savedSale);
        return mapToResponseDto(updatedSale);
    }

    @Override
    public SaleResponseDto cancelSale(Long saleId) {
        Sale sale = getSaleOrThrow(saleId);

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new RuntimeException("La venta ya está cancelada");
        }

        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);

        for (SaleItem item : items) {
            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductId(sale.getBranch().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "No existe inventario para revertir el producto " + item.getProduct().getName()
                    ));

            inventory.setStockQuantity(safe(inventory.getStockQuantity()).add(safe(item.getQuantity())));
            branchInventoryRepository.save(inventory);

            InventoryMovement movement = InventoryMovement.builder()
                    .branch(sale.getBranch())
                    .product(item.getProduct())
                    .movementType(MovementType.RETURN_IN)
                    .quantity(item.getQuantity())
                    .unitCost(safe(inventory.getAverageCost()))
                    .totalCost(safe(item.getQuantity()).multiply(safe(inventory.getAverageCost())).setScale(2, RoundingMode.HALF_UP))
                    .reason("Cancelación de venta #" + sale.getId())
                    .referenceType(ReferenceType.SALE)
                    .referenceId(sale.getId())
                    .performedBy(sale.getCreatedBy())
                    .movementDate(LocalDateTime.now())
                    .build();

            inventoryMovementRepository.save(movement);
        }

        sale.setStatus(SaleStatus.CANCELLED);
        Sale updatedSale = saleRepository.save(sale);
        return mapToResponseDto(updatedSale);
    }

    private Sale getSaleOrThrow(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));
    }

    private SaleResponseDto mapToResponseDto(Sale sale) {
        List<SaleItemResponseDto> items = saleItemRepository.findBySaleId(sale.getId())
                .stream()
                .map(item -> SaleItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .unitId(item.getUnit().getId())
                        .unitName(item.getUnit().getUnitName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountPercent(item.getDiscountPercent())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return SaleResponseDto.builder()
                .id(sale.getId())
                .branchId(sale.getBranch().getId())
                .branchName(sale.getBranch().getName())
                .saleDate(sale.getSaleDate())
                .customerName(sale.getCustomerName())
                .status(sale.getStatus())
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .total(sale.getTotal())
                .createdByUserId(sale.getCreatedBy().getId())
                .createdByUserName(sale.getCreatedBy().getFullName())
                .items(items)
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}