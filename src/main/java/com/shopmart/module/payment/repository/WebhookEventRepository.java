package com.shopmart.module.payment.repository;

import com.shopmart.module.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByProviderAndEventId(String provider, String eventId);
}
