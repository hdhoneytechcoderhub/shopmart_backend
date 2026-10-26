package com.shopmart.module.customer.repository;

import com.shopmart.module.customer.entity.CustomerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {
    Page<CustomerNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
