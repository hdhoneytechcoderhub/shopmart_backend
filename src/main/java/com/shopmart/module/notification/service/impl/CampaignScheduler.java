package com.shopmart.module.notification.service.impl;

import com.shopmart.module.notification.service.NotificationCampaignService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for due SCHEDULED campaigns and sends them. Disabled via app.reports.scheduled.enabled=false
 *  is NOT what gates this — that flag is for the Phase 6 report scheduler; campaigns have their
 *  own poll interval (app.notifications.campaigns.poll-interval-ms) but no separate on/off switch
 *  since a scheduled campaign with no scheduler running would simply never send, which is worse
 *  than surprising — it's silently broken.
 *  @SchedulerLock here is what actually prevents a campaign being sent twice if this scales to
 *  >1 Railway replica — without it, two replicas could both read the same due campaign before
 *  either flips its status to SENDING. */
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final NotificationCampaignService campaignService;

    @Scheduled(fixedDelayString = "${app.notifications.campaigns.poll-interval-ms:60000}")
    @SchedulerLock(name = "campaignScheduler", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void run() {
        campaignService.processDueCampaigns();
    }
}
