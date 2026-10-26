package com.shopmart.module.compliance.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "compliance_documents",
        indexes = @Index(name = "idx_compliance_status", columnList = "status"))
public class ComplianceDocument extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(nullable = false)
    private String title;

    @Column(name = "document_url", nullable = false)
    private String documentUrl;

    /** Set only when the document was uploaded via FileStorageService (Phase 5 integration) —
     *  null for documents whose URL was supplied directly via the JSON upload endpoint. */
    @Column(name = "document_storage_key")
    private String documentStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceStatus status = ComplianceStatus.PENDING;

    @Column(length = 1000)
    private String reason;

    @Column(name = "certificate_url")
    private String certificateUrl;
}
