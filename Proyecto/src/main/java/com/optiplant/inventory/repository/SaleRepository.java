package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.Sale;
import com.optiplant.inventory.enumtype.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByBranchId(Long branchId);
    List<Sale> findByCreatedById(Long userId);
    List<Sale> findByStatus(SaleStatus status);
}