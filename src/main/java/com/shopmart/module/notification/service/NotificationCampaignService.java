package com.shopmart.module.notification.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.notification.dto.CampaignRequest;
import com.shopmart.module.notification.dto.CampaignResponse;
import com.shopmart.module.notification.dto.CampaignStatsResponse;
import com.shopmart.module.notification.entity.CampaignStatus;
import org.springframework.data.domain.Pageable;

public interface NotificationCampaignService {
    CampaignResponse create(CampaignRequest request, Long adminId);
    CampaignResponse update(Long id, CampaignRequest request);
    void delete(Long id);
    CampaignResponse get(Long id);
    PageResponse<CampaignResponse> list(CampaignStatus status, Pageable pageable);
    CampaignResponse sendNow(Long id);
    CampaignResponse cancel(Long id);
    CampaignStatsResponse stats();

    /** Called by the scheduler; sends every SCHEDULED campaign whose scheduledAt has passed. */
    void processDueCampaigns();
}
