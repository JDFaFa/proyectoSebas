package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.TransferPriority;
import com.optiplant.inventory.enumtype.TransferStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDto {

    private Long id;
    private Long requestingBranchId;
    private String requestingBranchName;
    private Long sourceBranchId;
    private String sourceBranchName;
    private Long destinationBranchId;
    private String destinationBranchName;
    private TransferStatus status;
    private TransferPriority priority;
    private Long requestedByUserId;
    private String requestedByUserName;
    private Long approvedByUserId;
    private String approvedByUserName;
    private String transportCompany;
    private String trackingNumber;
    private LocalDateTime estimatedArrivalDate;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private String notes;
    private List<TransferItemResponseDto> items;
    private List<TransferStatusHistoryResponseDto> statusHistory;
}