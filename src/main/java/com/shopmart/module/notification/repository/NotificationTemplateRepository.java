package com.shopmart.module.notification.repository;

import com.shopmart.module.notification.entity.NotificationChannel;
import com.shopmart.module.notification.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    boolean existsByCode(String code);
    Optional<NotificationTemplate> findByCode(String code);
    Page<NotificationTemplate> findByChannel(NotificationChannel channel, Pageable pageable);
}
