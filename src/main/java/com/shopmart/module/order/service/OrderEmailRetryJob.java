package com.shopmart.module.order.service;

import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retries confirmation/admin emails for PAID orders whose send timestamps are still null
 * (mail provider outage, missing config at payment time, etc.). Runs independently of the
 * payment transaction, so a confirmed order is never lost because email delivery failed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEmailRetryJob {

    private final OrderRepository orderRepository;
    private final OrderEmailService orderEmailService;

    @Scheduled(fixedDelayString = "${app.orders.email-retry-interval-ms:300000}")
    @SchedulerLock(name = "orderEmailRetryJob", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void retryPendingEmails() {
        List<Order> pending = orderRepository.findPaidOrdersPendingEmails(PageRequest.of(0, 25));
        if (pending.isEmpty()) return;
        log.info("[ORDER_EMAIL] retrying emails for {} paid order(s)", pending.size());
        for (Order order : pending) {
            try {
                orderEmailService.sendForPaidOrder(order.getId());
            } catch (Exception e) {
                log.error("[ORDER_EMAIL] retry failed for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
