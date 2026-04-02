package com.optiplant.inventory.repository;

import com.optiplant.inventory.entity.Role;
import com.optiplant.inventory.enumtype.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}