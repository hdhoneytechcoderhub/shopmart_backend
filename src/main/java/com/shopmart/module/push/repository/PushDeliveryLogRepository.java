package com.shopmart.module.push.repository;

import com.shopmart.module.push.entity.PushDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeliveryLogRepository extends JpaRepository<PushDeliveryLog, Long> {
    Page<PushDeliveryLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
