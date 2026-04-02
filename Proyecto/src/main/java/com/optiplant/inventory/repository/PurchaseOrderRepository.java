package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.PurchaseOrder;
import com.optiplant.inventory.enumtype.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByBranchId(Long branchId);
    List<PurchaseOrder> findBySupplierId(Long supplierId);
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);
}