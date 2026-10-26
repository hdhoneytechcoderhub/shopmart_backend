package com.shopmart.module.user.dto;

public record AddressResponse(
        Long id, String label, String fullName, String phone,
        String line1, String line2, String city, String state,
        String postalCode, String country, boolean isDefault
) {}
