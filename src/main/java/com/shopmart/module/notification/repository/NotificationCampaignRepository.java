package com.shopmart.module.notification.repository;

import com.shopmart.module.notification.entity.CampaignStatus;
import com.shopmart.module.notification.entity.NotificationCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, Long> {
    Page<NotificationCampaign> findByStatus(CampaignStatus status, Pageable pageable);
    List<NotificationCampaign> findByStatusAndScheduledAtLessThanEqual(CampaignStatus status, Instant now);
}
