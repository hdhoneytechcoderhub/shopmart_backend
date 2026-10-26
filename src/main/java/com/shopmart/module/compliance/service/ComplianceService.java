package com.shopmart.module.compliance.service;

import com.shopmart.module.compliance.dto.ComplianceDecisionRequest;
import com.shopmart.module.compliance.dto.ComplianceResponse;
import com.shopmart.module.compliance.dto.ComplianceUploadRequest;

import java.util.List;

public interface ComplianceService {
    ComplianceResponse upload(Long userId, ComplianceUploadRequest req);
    /** Real file upload variant (Phase 5 integration) — uploads via FileStorageService instead
     *  of requiring the caller to already have a URL. */
    ComplianceResponse uploadFile(Long userId, String title, org.springframework.web.multipart.MultipartFile file);
    List<ComplianceResponse> list(String status);
    ComplianceResponse get(Long id);
    ComplianceResponse approve(Long id, ComplianceDecisionRequest req);
    ComplianceResponse reject(Long id, ComplianceDecisionRequest req);
    String certificate(Long id);
}
