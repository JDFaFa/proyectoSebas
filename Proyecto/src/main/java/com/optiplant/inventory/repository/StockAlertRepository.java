package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.StockAlert;
import com.optiplant.inventory.enumtype.AlertStatus;
import com.optiplant.inventory.enumtype.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    List<StockAlert> findByBranchId(Long branchId);

    List<StockAlert> findByProductId(Long productId);

    List<StockAlert> findByStatus(AlertStatus status);

    List<StockAlert> findByAlertType(AlertType alertType);

    List<StockAlert> findByBranchIdAndStatus(Long branchId, AlertStatus status);

    Optional<StockAlert> findByBranchIdAndProductIdAndAlertTypeAndStatus(
            Long branchId,
            Long productId,
            AlertType alertType,
            AlertStatus status
    );
}