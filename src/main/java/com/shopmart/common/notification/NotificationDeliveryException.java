package com.shopmart.common.notification;

public class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException() {
        super("Notification delivery failed");
    }

    public NotificationDeliveryException(Throwable cause) {
        super("Notification delivery failed", cause);
    }
}
