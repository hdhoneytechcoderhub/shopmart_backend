package com.shopmart.module.notification.dto;

public record CampaignStatsResponse(
        long totalCampaigns, long draft, long scheduled, long sent, long cancelled, long failed,
        long totalRecipientsReached
) {}
