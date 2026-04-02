package com.optiplant.inventory.service;

import com.optiplant.inventory.dto.TransferRequestDto;
import com.optiplant.inventory.dto.TransferResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface TransferService {

    List<TransferResponseDto> getAllTransfers();

    TransferResponseDto getTransferById(Long id);

    TransferResponseDto createTransfer(TransferRequestDto requestDto);

    TransferResponseDto approveTransfer(Long transferId, Long approvedByUserId);

    TransferResponseDto shipTransfer(Long transferId);

    TransferResponseDto receiveTransfer(Long transferId, Long changedByUserId);

    TransferResponseDto receiveTransferPartial(Long transferId, Long changedByUserId, Long productId, BigDecimal receivedQuantity, String observation);

    TransferResponseDto cancelTransfer(Long transferId, Long changedByUserId, String comments);
}