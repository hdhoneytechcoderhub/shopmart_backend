package com.shopmart.module.order.event;

/** Published only after a payment has been verified and the order confirmed as PAID. */
public record OrderPaidEvent(Long orderId) {}
