package com.shopmart.common.notification;

public interface PushSender {
    /** userId identifies whose registered device token(s) to push to; the actual token lookup
     *  (device token storage) is provider-specific and not modeled here. */
    void send(Long userId, String title, String message, String link);
}
