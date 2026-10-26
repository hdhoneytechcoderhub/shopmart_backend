package com.shopmart.module.audit.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    /** Record an audit entry. Safe to call from anywhere; never throws to the caller. */
    void log(Long userId, String action, String entityType, Long entityId, String detail);

    PageResponse<AuditLogResponse> list(Long userId, String action, Pageable pageable);
}
