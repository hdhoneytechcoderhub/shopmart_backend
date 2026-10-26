package com.shopmart.module.permission.repository;

import com.shopmart.module.permission.entity.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, Long> {
    boolean existsByName(String name);
}
