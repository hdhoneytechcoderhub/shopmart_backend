package com.shopmart.module.contact.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.contact.dto.ContactRequest;
import com.shopmart.module.contact.dto.ContactResponse;
import com.shopmart.module.contact.service.ContactService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
@Tag(name = "Contact")
public class ContactController {

    private final ContactService service;

    /** Public: submit a contact form. */
    @PostMapping
    public ApiResponse<ContactResponse> submit(@Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok("Message received", service.submit(request));
    }

    /** Admin: list submitted messages. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<ContactResponse>> list() {
        return ApiResponse.ok(service.list());
    }
}
