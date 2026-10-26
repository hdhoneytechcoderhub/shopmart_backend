package com.shopmart.module.notification.repository;

import com.shopmart.module.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    void markAllRead(Long userId);

    // ---- Admin history/filter (Phase 1: Notification History) ----
    @Query("""
            select n from Notification n
            where (:userId is null or n.userId = :userId)
              and (:type is null or n.type = :type)
              and (:from is null or n.createdAt >= :from)
              and (:to is null or n.createdAt <= :to)
            order by n.createdAt desc
            """)
    Page<Notification> search(@org.springframework.data.repository.query.Param("userId") Long userId,
                               @org.springframework.data.repository.query.Param("type") com.shopmart.module.notification.entity.NotificationType type,
                               @org.springframework.data.repository.query.Param("from") java.time.Instant from,
                               @org.springframework.data.repository.query.Param("to") java.time.Instant to,
                               Pageable pageable);
}
