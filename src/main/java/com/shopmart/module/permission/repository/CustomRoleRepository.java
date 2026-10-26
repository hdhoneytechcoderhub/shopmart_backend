package com.shopmart.module.permission.repository;

import com.shopmart.module.permission.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {
    Optional<CustomRole> findByName(String name);
    boolean existsByName(String name);
}
