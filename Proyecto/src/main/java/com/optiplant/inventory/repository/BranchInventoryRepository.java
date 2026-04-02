package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.BranchInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {
    Optional<BranchInventory> findByBranchIdAndProductId(Long branchId, Long productId);
    List<BranchInventory> findByBranchId(Long branchId);
}