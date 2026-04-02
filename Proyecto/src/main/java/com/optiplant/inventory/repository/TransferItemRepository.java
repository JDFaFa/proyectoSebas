package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.TransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferItemRepository extends JpaRepository<TransferItem, Long> {
    List<TransferItem> findByTransferId(Long transferId);
    List<TransferItem> findByProductId(Long productId);
}