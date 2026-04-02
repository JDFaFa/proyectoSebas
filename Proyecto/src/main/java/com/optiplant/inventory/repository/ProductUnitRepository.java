package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long> {
    List<ProductUnit> findByProductId(Long productId);
}