package com.optiplant.inventory.controller;

import com.optiplant.inventory.dto.TransferRequestDto;
import com.optiplant.inventory.dto.TransferResponseDto;
import com.optiplant.inventory.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    public ResponseEntity<List<TransferResponseDto>> getAllTransfers() {
        return ResponseEntity.ok(transferService.getAllTransfers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponseDto> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getTransferById(id));
    }

    @PostMapping
    public ResponseEntity<TransferResponseDto> createTransfer(@Valid @RequestBody TransferRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.createTransfer(requestDto));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TransferResponseDto> approveTransfer(
            @PathVariable Long id,
            @RequestParam Long approvedByUserId
    ) {
        return ResponseEntity.ok(transferService.approveTransfer(id, approvedByUserId));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<TransferResponseDto> shipTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.shipTransfer(id));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<TransferResponseDto> receiveTransfer(
            @PathVariable Long id,
            @RequestParam Long changedByUserId
    ) {
        return ResponseEntity.ok(transferService.receiveTransfer(id, changedByUserId));
    }

    @PostMapping("/{id}/receive-partial")
    public ResponseEntity<TransferResponseDto> receiveTransferPartial(
            @PathVariable Long id,
            @RequestParam Long changedByUserId,
            @RequestParam Long productId,
            @RequestParam BigDecimal receivedQuantity,
            @RequestParam(required = false) String observation
    ) {
        return ResponseEntity.ok(
                transferService.receiveTransferPartial(id, changedByUserId, productId, receivedQuantity, observation)
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransferResponseDto> cancelTransfer(
            @PathVariable Long id,
            @RequestParam Long changedByUserId,
            @RequestParam(required = false) String comments
    ) {
        return ResponseEntity.ok(transferService.cancelTransfer(id, changedByUserId, comments));
    }
}