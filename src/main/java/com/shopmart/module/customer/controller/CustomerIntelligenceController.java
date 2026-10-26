package com.shopmart.module.customer.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.customer.dto.*;
import com.shopmart.module.customer.service.CustomerIntelligenceService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Customer Intelligence")
public class CustomerIntelligenceController {

    private final CustomerIntelligenceService service;

    @GetMapping("/{id}/insights")
    public ApiResponse<CustomerInsightsResponse> insights(@PathVariable Long id) {
        return ApiResponse.ok(service.insights(id));
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<List<CustomerTimelineEvent>> timeline(@PathVariable Long id,
                                                              @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(service.timeline(id, limit));
    }

    @GetMapping("/{id}/activity")
    public ApiResponse<List<CustomerTimelineEvent>> activity(@PathVariable Long id,
                                                              @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.recentActivity(id, limit));
    }

    @GetMapping("/{id}/segment")
    public ApiResponse<CustomerSegment> segment(@PathVariable Long id) {
        return ApiResponse.ok(service.segmentOf(id));
    }

    @GetMapping("/segments/{segment}")
    public ApiResponse<PageResponse<CustomerInsightsResponse>> bySegment(
            @PathVariable CustomerSegment segment,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.bySegment(segment, pageable));
    }

    @PostMapping("/{id}/notes")
    public ApiResponse<CustomerNoteResponse> addNote(@PathVariable Long id,
                                                      @Valid @RequestBody CustomerNoteRequest request) {
        return ApiResponse.ok("Note added", service.addNote(id, SecurityUtils.currentUserId(), request.note()));
    }

    @GetMapping("/{id}/notes")
    public ApiResponse<PageResponse<CustomerNoteResponse>> notes(@PathVariable Long id,
                                                                  @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.listNotes(id, pageable));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    public ApiResponse<Void> deleteNote(@PathVariable Long id, @PathVariable Long noteId) {
        service.deleteNote(id, noteId);
        return ApiResponse.message("Note deleted");
    }
}
