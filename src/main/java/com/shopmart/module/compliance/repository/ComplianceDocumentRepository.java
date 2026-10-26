package com.shopmart.module.compliance.repository;

import com.shopmart.module.compliance.entity.ComplianceDocument;
import com.shopmart.module.compliance.entity.ComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument, Long> {
    List<ComplianceDocument> findByStatusOrderByCreatedAtDesc(ComplianceStatus status);
    List<ComplianceDocument> findAllByOrderByCreatedAtDesc();
}
