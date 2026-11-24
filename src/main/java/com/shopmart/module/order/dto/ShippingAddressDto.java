package com.shopmart.module.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ShippingAddressDto(
        @NotBlank String name,
        @NotBlank String phone,
        @Email String email,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank String country
) {}
