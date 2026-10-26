package com.shopmart.module.customer.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.customer.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerIntelligenceService {

    CustomerInsightsResponse insights(Long customerId);

    List<CustomerTimelineEvent> timeline(Long customerId, int limit);

    /** Same underlying data as timeline(), exposed as its own endpoint per the "Customer
     *  Activity API" requirement — kept as a thin alias rather than a second implementation
     *  so the two can never drift out of sync. */
    List<CustomerTimelineEvent> recentActivity(Long customerId, int limit);

    CustomerSegment segmentOf(Long customerId);

    /** Every customer currently in the given segment. Computes segment membership across all
     *  users in memory — fine at small/medium scale; for a large customer base this should move
     *  to a scheduled job that materializes segment membership into a table instead of computing
     *  live on every request. */
    PageResponse<CustomerInsightsResponse> bySegment(CustomerSegment segment, Pageable pageable);

    /** Unpaginated customer ids in a segment — used internally by campaign targeting rather
     *  than by any HTTP endpoint (a campaign send needs the full recipient list, not a page). */
    List<Long> customerIdsInSegment(CustomerSegment segment);

    CustomerNoteResponse addNote(Long customerId, Long authorId, String note);
    PageResponse<CustomerNoteResponse> listNotes(Long customerId, Pageable pageable);
    void deleteNote(Long customerId, Long noteId);
}
