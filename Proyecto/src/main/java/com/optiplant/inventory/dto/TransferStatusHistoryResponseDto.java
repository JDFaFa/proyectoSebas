package com.optiplant.inventory.dto;

import com.optiplant.inventory.enumtype.TransferStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferStatusHistoryResponseDto {

    private Long id;
    private TransferStatus status;
    private Long changedByUserId;
    private String changedByUserName;
    private LocalDateTime changedAt;
    private String comments;
}