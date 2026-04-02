package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.Transfer;
import com.optiplant.inventory.enumtype.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findBySourceBranchId(Long sourceBranchId);
    List<Transfer> findByDestinationBranchId(Long destinationBranchId);
    List<Transfer> findByRequestingBranchId(Long requestingBranchId);
    List<Transfer> findByRequestedById(Long userId);
    List<Transfer> findByStatus(TransferStatus status);
}