package com.shopmart.module.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank @Email(message = "Valid email is required") String email,
        String phone,
        @NotBlank(message = "Subject is required") String subject,
        @NotBlank @Size(max = 4000, message = "Message is too long") String message
) {}
