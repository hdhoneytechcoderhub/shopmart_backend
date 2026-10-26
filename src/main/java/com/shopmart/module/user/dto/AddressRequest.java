package com.shopmart.module.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Phone is required") String phone,
        @NotBlank(message = "Address line 1 is required") String line1,
        String line2,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "State is required") String state,
        @NotBlank(message = "Postal code is required") String postalCode,
        @NotBlank(message = "Country is required") String country,
        Boolean isDefault
) {}
