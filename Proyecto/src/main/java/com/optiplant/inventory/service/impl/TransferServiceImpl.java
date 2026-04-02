package com.optiplant.inventory.service.impl;

import com.optiplant.inventory.dto.*;
import com.optiplant.inventory.entity.*;
import com.optiplant.inventory.enumtype.*;
import com.optiplant.inventory.repository.*;
import com.optiplant.inventory.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final TransferItemRepository transferItemRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final StockAlertRepository stockAlertRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponseDto> getAllTransfers() {
        return transferRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponseDto getTransferById(Long id) {
        return mapToResponseDto(getTransferOrThrow(id));
    }

    @Override
    public TransferResponseDto createTransfer(TransferRequestDto requestDto) {
        Branch requestingBranch = getBranchOrThrow(requestDto.getRequestingBranchId());
        Branch sourceBranch = getBranchOrThrow(requestDto.getSourceBranchId());
        Branch destinationBranch = getBranchOrThrow(requestDto.getDestinationBranchId());
        User requestedBy = getUserOrThrow(requestDto.getRequestedByUserId());

        if (sourceBranch.getId().equals(destinationBranch.getId())) {
            throw new RuntimeException("La sucursal origen y destino no pueden ser la misma");
        }

        Transfer transfer = Transfer.builder()
                .requestingBranch(requestingBranch)
                .sourceBranch(sourceBranch)
                .destinationBranch(destinationBranch)
                .status(TransferStatus.REQUESTED)
                .priority(requestDto.getPriority() != null ? requestDto.getPriority() : TransferPriority.MEDIUM)
                .requestedBy(requestedBy)
                .transportCompany(requestDto.getTransportCompany())
                .trackingNumber(requestDto.getTrackingNumber())
                .estimatedArrivalDate(requestDto.getEstimatedArrivalDate())
                .notes(requestDto.getNotes())
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        for (TransferItemRequestDto itemDto : requestDto.getItems()) {
            Product product = getProductOrThrow(itemDto.getProductId());
            ProductUnit unit = getUnitOrThrow(itemDto.getUnitId());

            if (!unit.getProduct().getId().equals(product.getId())) {
                throw new RuntimeException("La unidad no corresponde al producto seleccionado");
            }

            TransferItem item = TransferItem.builder()
                    .transfer(savedTransfer)
                    .product(product)
                    .unit(unit)
                    .requestedQuantity(itemDto.getRequestedQuantity())
                    .approvedQuantity(BigDecimal.ZERO)
                    .shippedQuantity(BigDecimal.ZERO)
                    .receivedQuantity(BigDecimal.ZERO)
                    .missingQuantity(BigDecimal.ZERO)
                    .build();

            transferItemRepository.save(item);
        }

        saveStatusHistory(savedTransfer, TransferStatus.REQUESTED, requestedBy, "Transferencia solicitada");

        return mapToResponseDto(savedTransfer);
    }

    @Override
    public TransferResponseDto approveTransfer(Long transferId, Long approvedByUserId) {
        Transfer transfer = getTransferOrThrow(transferId);
        User approvedBy = getUserOrThrow(approvedByUserId);

        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            throw new RuntimeException("Solo se pueden aprobar transferencias en estado REQUESTED");
        }

        List<TransferItem> items = transferItemRepository.findByTransferId(transferId);

        for (TransferItem item : items) {
            BranchInventory sourceInventory = branchInventoryRepository
                    .findByBranchIdAndProductId(transfer.getSourceBranch().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No existe inventario en sucursal origen para el producto " + item.getProduct().getName()));

            BigDecimal availableStock = safe(sourceInventory.getStockQuantity());
            BigDecimal approved = item.getRequestedQuantity().min(availableStock);

            item.setApprovedQuantity(approved);
            transferItemRepository.save(item);
        }

        transfer.setApprovedBy(approvedBy);
        transfer.setStatus(TransferStatus.APPROVED);
        Transfer savedTransfer = transferRepository.save(transfer);

        saveStatusHistory(savedTransfer, TransferStatus.APPROVED, approvedBy, "Transferencia aprobada");

        return mapToResponseDto(savedTransfer);
    }

    @Override
    public TransferResponseDto shipTransfer(Long transferId) {
        Transfer transfer = getTransferOrThrow(transferId);

        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw new RuntimeException("Solo se pueden despachar transferencias en estado APPROVED");
        }

        List<TransferItem> items = transferItemRepository.findByTransferId(transferId);

        for (TransferItem item : items) {
            if (safe(item.getApprovedQuantity()).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BranchInventory sourceInventory = branchInventoryRepository
                    .findByBranchIdAndProductId(transfer.getSourceBranch().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No existe inventario en sucursal origen para el producto " + item.getProduct().getName()));

            BigDecimal stock = safe(sourceInventory.getStockQuantity());
            BigDecimal quantityToShip = safe(item.getApprovedQuantity());

            if (stock.compareTo(quantityToShip) < 0) {
                throw new RuntimeException("Stock insuficiente para despachar el producto " + item.getProduct().getName());
            }

            sourceInventory.setStockQuantity(stock.subtract(quantityToShip));
            branchInventoryRepository.save(sourceInventory);

            item.setShippedQuantity(quantityToShip);
            transferItemRepository.save(item);

            InventoryMovement movement = InventoryMovement.builder()
                    .branch(transfer.getSourceBranch())
                    .product(item.getProduct())
                    .movementType(MovementType.TRANSFER_OUT)
                    .quantity(quantityToShip)
                    .unitCost(safe(sourceInventory.getAverageCost()))
                    .totalCost(quantityToShip.multiply(safe(sourceInventory.getAverageCost())))
                    .reason("Despacho de transferencia #" + transfer.getId())
                    .referenceType(ReferenceType.TRANSFER)
                    .referenceId(transfer.getId())
                    .performedBy(transfer.getApprovedBy() != null ? transfer.getApprovedBy() : transfer.getRequestedBy())
                    .movementDate(LocalDateTime.now())
                    .build();

            inventoryMovementRepository.save(movement);
        }

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setShippedAt(LocalDateTime.now());
        Transfer savedTransfer = transferRepository.save(transfer);

        saveStatusHistory(savedTransfer, TransferStatus.IN_TRANSIT, transfer.getApprovedBy() != null ? transfer.getApprovedBy() : transfer.getRequestedBy(), "Transferencia despachada");

        return mapToResponseDto(savedTransfer);
    }

    @Override
    public TransferResponseDto receiveTransfer(Long transferId, Long changedByUserId) {
        Transfer transfer = getTransferOrThrow(transferId);
        User changedBy = getUserOrThrow(changedByUserId);

        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new RuntimeException("Solo se pueden recibir transferencias en tránsito");
        }

        List<TransferItem> items = transferItemRepository.findByTransferId(transferId);

        for (TransferItem item : items) {
            BigDecimal shippedQuantity = safe(item.getShippedQuantity());

            if (shippedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BranchInventory destinationInventory = branchInventoryRepository
                    .findByBranchIdAndProductId(transfer.getDestinationBranch().getId(), item.getProduct().getId())
                    .orElseGet(() -> BranchInventory.builder()
                            .branch(transfer.getDestinationBranch())
                            .product(item.getProduct())
                            .stockQuantity(BigDecimal.ZERO)
                            .reservedQuantity(BigDecimal.ZERO)
                            .averageCost(BigDecimal.ZERO)
                            .minimumStock(BigDecimal.ZERO)
                            .build());

            destinationInventory.setStockQuantity(safe(destinationInventory.getStockQuantity()).add(shippedQuantity));
            branchInventoryRepository.save(destinationInventory);

            item.setReceivedQuantity(shippedQuantity);
            item.setMissingQuantity(BigDecimal.ZERO);
            transferItemRepository.save(item);

            InventoryMovement movement = InventoryMovement.builder()
                    .branch(transfer.getDestinationBranch())
                    .product(item.getProduct())
                    .movementType(MovementType.TRANSFER_IN)
                    .quantity(shippedQuantity)
                    .unitCost(safe(destinationInventory.getAverageCost()))
                    .totalCost(shippedQuantity.multiply(safe(destinationInventory.getAverageCost())))
                    .reason("Recepción de transferencia #" + transfer.getId())
                    .referenceType(ReferenceType.TRANSFER)
                    .referenceId(transfer.getId())
                    .performedBy(changedBy)
                    .movementDate(LocalDateTime.now())
                    .build();

            inventoryMovementRepository.save(movement);
        }

        transfer.setStatus(TransferStatus.RECEIVED_COMPLETE);
        transfer.setReceivedAt(LocalDateTime.now());
        Transfer savedTransfer = transferRepository.save(transfer);

        saveStatusHistory(savedTransfer, TransferStatus.RECEIVED_COMPLETE, changedBy, "Transferencia recibida completa");

        return mapToResponseDto(savedTransfer);
    }

    @Override
    public TransferResponseDto receiveTransferPartial(Long transferId, Long changedByUserId, Long productId, BigDecimal receivedQuantity, String observation) {
        Transfer transfer = getTransferOrThrow(transferId);
        User changedBy = getUserOrThrow(changedByUserId);

        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new RuntimeException("Solo se pueden recibir parcialmente transferencias en tránsito");
        }

        TransferItem item = transferItemRepository.findByTransferId(transferId)
                .stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Producto no encontrado en la transferencia"));

        BigDecimal shipped = safe(item.getShippedQuantity());

        if (receivedQuantity.compareTo(BigDecimal.ZERO) < 0 || receivedQuantity.compareTo(shipped) > 0) {
            throw new RuntimeException("La cantidad recibida parcial es inválida");
        }

        BigDecimal missing = shipped.subtract(receivedQuantity);

        BranchInventory destinationInventory = branchInventoryRepository
                .findByBranchIdAndProductId(transfer.getDestinationBranch().getId(), item.getProduct().getId())
                .orElseGet(() -> BranchInventory.builder()
                        .branch(transfer.getDestinationBranch())
                        .product(item.getProduct())
                        .stockQuantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .averageCost(BigDecimal.ZERO)
                        .minimumStock(BigDecimal.ZERO)
                        .build());

        destinationInventory.setStockQuantity(safe(destinationInventory.getStockQuantity()).add(receivedQuantity));
        branchInventoryRepository.save(destinationInventory);

        item.setReceivedQuantity(receivedQuantity);
        item.setMissingQuantity(missing);
        item.setObservation(observation);
        transferItemRepository.save(item);

        InventoryMovement movement = InventoryMovement.builder()
                .branch(transfer.getDestinationBranch())
                .product(item.getProduct())
                .movementType(MovementType.TRANSFER_IN)
                .quantity(receivedQuantity)
                .unitCost(safe(destinationInventory.getAverageCost()))
                .totalCost(receivedQuantity.multiply(safe(destinationInventory.getAverageCost())))
                .reason("Recepción parcial de transferencia #" + transfer.getId())
                .referenceType(ReferenceType.TRANSFER)
                .referenceId(transfer.getId())
                .performedBy(changedBy)
                .movementDate(LocalDateTime.now())
                .build();

        inventoryMovementRepository.save(movement);

        stockAlertRepository.save(
                StockAlert.builder()
                        .branch(transfer.getDestinationBranch())
                        .product(item.getProduct())
                        .alertType(AlertType.MISSING_ITEMS)
                        .message("La transferencia #" + transfer.getId() + " presenta faltantes")
                        .status(AlertStatus.ACTIVE)
                        .build()
        );

        transfer.setStatus(TransferStatus.RECEIVED_PARTIAL);
        transfer.setReceivedAt(LocalDateTime.now());
        Transfer savedTransfer = transferRepository.save(transfer);

        saveStatusHistory(savedTransfer, TransferStatus.RECEIVED_PARTIAL, changedBy, "Transferencia recibida parcialmente");

        return mapToResponseDto(savedTransfer);
    }

    @Override
    public TransferResponseDto cancelTransfer(Long transferId, Long changedByUserId, String comments) {
        Transfer transfer = getTransferOrThrow(transferId);
        User changedBy = getUserOrThrow(changedByUserId);

        if (transfer.getStatus() == TransferStatus.RECEIVED_COMPLETE || transfer.getStatus() == TransferStatus.RECEIVED_PARTIAL) {
            throw new RuntimeException("No se puede cancelar una transferencia ya recibida");
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        Transfer savedTransfer = transferRepository.save(transfer);

        saveStatusHistory(savedTransfer, TransferStatus.CANCELLED, changedBy, comments != null ? comments : "Transferencia cancelada");

        return mapToResponseDto(savedTransfer);
    }

    private void saveStatusHistory(Transfer transfer, TransferStatus status, User changedBy, String comments) {
        TransferStatusHistory history = TransferStatusHistory.builder()
                .transfer(transfer)
                .status(status)
                .changedBy(changedBy)
                .comments(comments)
                .build();

        transferStatusHistoryRepository.save(history);
    }

    private TransferResponseDto mapToResponseDto(Transfer transfer) {
        List<TransferItemResponseDto> items = transferItemRepository.findByTransferId(transfer.getId())
                .stream()
                .map(item -> TransferItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .unitId(item.getUnit().getId())
                        .unitName(item.getUnit().getUnitName())
                        .requestedQuantity(item.getRequestedQuantity())
                        .approvedQuantity(item.getApprovedQuantity())
                        .shippedQuantity(item.getShippedQuantity())
                        .receivedQuantity(item.getReceivedQuantity())
                        .missingQuantity(item.getMissingQuantity())
                        .observation(item.getObservation())
                        .build())
                .toList();

        List<TransferStatusHistoryResponseDto> history = transferStatusHistoryRepository
                .findByTransferIdOrderByChangedAtAsc(transfer.getId())
                .stream()
                .map(h -> TransferStatusHistoryResponseDto.builder()
                        .id(h.getId())
                        .status(h.getStatus())
                        .changedByUserId(h.getChangedBy().getId())
                        .changedByUserName(h.getChangedBy().getFullName())
                        .changedAt(h.getChangedAt())
                        .comments(h.getComments())
                        .build())
                .toList();

        return TransferResponseDto.builder()
                .id(transfer.getId())
                .requestingBranchId(transfer.getRequestingBranch().getId())
                .requestingBranchName(transfer.getRequestingBranch().getName())
                .sourceBranchId(transfer.getSourceBranch().getId())
                .sourceBranchName(transfer.getSourceBranch().getName())
                .destinationBranchId(transfer.getDestinationBranch().getId())
                .destinationBranchName(transfer.getDestinationBranch().getName())
                .status(transfer.getStatus())
                .priority(transfer.getPriority())
                .requestedByUserId(transfer.getRequestedBy().getId())
                .requestedByUserName(transfer.getRequestedBy().getFullName())
                .approvedByUserId(transfer.getApprovedBy() != null ? transfer.getApprovedBy().getId() : null)
                .approvedByUserName(transfer.getApprovedBy() != null ? transfer.getApprovedBy().getFullName() : null)
                .transportCompany(transfer.getTransportCompany())
                .trackingNumber(transfer.getTrackingNumber())
                .estimatedArrivalDate(transfer.getEstimatedArrivalDate())
                .shippedAt(transfer.getShippedAt())
                .receivedAt(transfer.getReceivedAt())
                .notes(transfer.getNotes())
                .items(items)
                .statusHistory(history)
                .build();
    }

    private Transfer getTransferOrThrow(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transferencia no encontrada con id: " + id));
    }

    private Branch getBranchOrThrow(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con id: " + id));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
    }

    private ProductUnit getUnitOrThrow(Long id) {
        return productUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada con id: " + id));
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}