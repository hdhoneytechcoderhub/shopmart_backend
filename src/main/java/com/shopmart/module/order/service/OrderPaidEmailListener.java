package com.shopmart.module.order.service;

import com.shopmart.module.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidEmailListener {

    private final OrderEmailService orderEmailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPaid(OrderPaidEvent event) {
        try {
            orderEmailService.sendForPaidOrder(event.orderId());
        } catch (Exception e) {
            // Email is best-effort; the confirmed order stays saved and the retry job picks
            // up any unsent flag on a later run.
            log.error("[ORDER_EMAIL] after-commit dispatch failed for order {}: {}",
                    event.orderId(), e.getMessage());
        }
    }
}
