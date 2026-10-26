package com.shopmart.module.notification.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.notification.dto.NotificationTemplateRequest;
import com.shopmart.module.notification.dto.NotificationTemplateResponse;
import com.shopmart.module.notification.entity.NotificationChannel;
import org.springframework.data.domain.Pageable;

public interface NotificationTemplateService {
    NotificationTemplateResponse create(NotificationTemplateRequest request);
    NotificationTemplateResponse update(Long id, NotificationTemplateRequest request);
    void delete(Long id);
    NotificationTemplateResponse get(Long id);
    NotificationTemplateResponse getByCode(String code);
    PageResponse<NotificationTemplateResponse> list(NotificationChannel channel, Pageable pageable);
}
