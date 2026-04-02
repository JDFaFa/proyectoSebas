package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.TransferStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferStatusHistoryRepository extends JpaRepository<TransferStatusHistory, Long> {
    List<TransferStatusHistory> findByTransferIdOrderByChangedAtAsc(Long transferId);
}