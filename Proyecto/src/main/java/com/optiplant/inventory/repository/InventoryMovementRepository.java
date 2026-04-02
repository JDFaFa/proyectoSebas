package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByBranchId(Long branchId);
    List<InventoryMovement> findByProductId(Long productId);
    List<InventoryMovement> findByBranchIdAndProductId(Long branchId, Long productId);
}