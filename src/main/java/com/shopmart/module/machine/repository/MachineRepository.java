package com.shopmart.module.machine.repository;

import com.shopmart.module.machine.entity.Machine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    Page<Machine> findByDeletedFalse(Pageable pageable);
    Optional<Machine> findByIdAndDeletedFalse(Long id);
    long countByDeletedFalse();
}
