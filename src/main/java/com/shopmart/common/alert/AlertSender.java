package com.shopmart.common.alert;

public interface AlertSender {
    void send(String subject, String message, AlertSeverity severity);
}
