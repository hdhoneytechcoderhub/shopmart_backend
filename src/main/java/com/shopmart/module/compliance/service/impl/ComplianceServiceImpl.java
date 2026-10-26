package com.shopmart.module.compliance.service.impl;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.compliance.dto.ComplianceDecisionRequest;
import com.shopmart.module.compliance.dto.ComplianceResponse;
import com.shopmart.module.compliance.dto.ComplianceUploadRequest;
import com.shopmart.module.compliance.entity.ComplianceDocument;
import com.shopmart.module.compliance.entity.ComplianceStatus;
import com.shopmart.module.compliance.repository.ComplianceDocumentRepository;
import com.shopmart.module.compliance.service.ComplianceService;
import com.shopmart.module.audit.service.AuditService;
import com.shopmart.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceDocumentRepository repository;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;

    @Override @Transactional
    public ComplianceResponse upload(Long userId, ComplianceUploadRequest req) {
        ComplianceDocument d = new ComplianceDocument();
        d.setUploadedBy(userId);
        d.setTitle(req.title());
        d.setDocumentUrl(req.documentUrl());
        d.setStatus(ComplianceStatus.PENDING);
        return toResponse(repository.save(d));
    }

    @Override @Transactional
    public ComplianceResponse uploadFile(Long userId, String title, org.springframework.web.multipart.MultipartFile file) {
        FileStorageService.UploadResult result = fileStorageService.upload(file, "compliance");
        ComplianceDocument d = new ComplianceDocument();
        d.setUploadedBy(userId);
        d.setTitle(title);
        d.setDocumentUrl(result.url());
        d.setDocumentStorageKey(result.storageKey());
        d.setStatus(ComplianceStatus.PENDING);
        return toResponse(repository.save(d));
    }

    @Override @Transactional(readOnly = true)
    public List<ComplianceResponse> list(String status) {
        List<ComplianceDocument> list = (status == null || status.isBlank())
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(parse(status));
        return list.stream().map(this::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public ComplianceResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override @Transactional
    public ComplianceResponse approve(Long id, ComplianceDecisionRequest req) {
        ComplianceDocument d = find(id);
        d.setStatus(ComplianceStatus.APPROVED);
        if (req != null) {
            d.setReason(req.reason());
            d.setCertificateUrl(req.certificateUrl());
        }
        ComplianceDocument saved = repository.save(d);
        auditService.log(null, "COMPLIANCE_APPROVED", "ComplianceDocument", saved.getId(), null);
        return toResponse(saved);
    }

    @Override @Transactional
    public ComplianceResponse reject(Long id, ComplianceDecisionRequest req) {
        ComplianceDocument d = find(id);
        d.setStatus(ComplianceStatus.REJECTED);
        if (req != null) d.setReason(req.reason());
        ComplianceDocument saved = repository.save(d);
        auditService.log(null, "COMPLIANCE_REJECTED", "ComplianceDocument", saved.getId(), req != null ? req.reason() : null);
        return toResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    public String certificate(Long id) {
        ComplianceDocument d = find(id);
        if (d.getStatus() != ComplianceStatus.APPROVED)
            throw new BadRequestException("Certificate is only available for approved documents");
        if (d.getCertificateUrl() == null || d.getCertificateUrl().isBlank())
            throw new ResourceNotFoundException("No certificate attached to document " + id);
        return d.getCertificateUrl();
    }

    private ComplianceDocument find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance document not found: " + id));
    }

    private ComplianceStatus parse(String s) {
        try { return ComplianceStatus.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new BadRequestException("Invalid status: " + s); }
    }

    private ComplianceResponse toResponse(ComplianceDocument d) {
        return new ComplianceResponse(d.getId(), d.getUploadedBy(), d.getTitle(), d.getDocumentUrl(),
                d.getStatus().name(), d.getReason(), d.getCertificateUrl(), d.getCreatedAt());
    }
}
