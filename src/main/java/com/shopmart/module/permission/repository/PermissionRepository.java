package com.shopmart.module.permission.repository;

import com.shopmart.module.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    boolean existsByCode(String code);
    List<Permission> findByModule(String module);
    List<Permission> findByGroupId(Long groupId);
}
