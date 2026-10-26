package com.shopmart.module.audit.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.audit.dto.AuditLogResponse;
import com.shopmart.module.audit.entity.AuditLog;
import com.shopmart.module.audit.repository.AuditLogRepository;
import com.shopmart.module.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;

    @Override
    @Transactional
    public void log(Long userId, String action, String entityType, Long entityId, String detail) {
        try {
            AuditLog a = new AuditLog();
            a.setUserId(userId);
            a.setAction(action);
            a.setEntityType(entityType);
            a.setEntityId(entityId);
            a.setDetail(detail);
            repository.save(a);
        } catch (Exception e) {
            // Auditing must never break the main flow.
            log.warn("Failed to write audit log [{}]: {}", action, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(Long userId, String action, Pageable pageable) {
        Page<AuditLog> page;
        if (userId != null) {
            page = repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (action != null && !action.isBlank()) {
            page = repository.findByActionOrderByCreatedAtDesc(action, pageable);
        } else {
            page = repository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getUserId(), a.getAction(), a.getEntityType(),
                a.getEntityId(), a.getDetail(), a.getCreatedAt());
    }
}
