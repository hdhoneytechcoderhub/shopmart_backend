package com.shopmart.module.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerNoteRequest(@NotBlank String note) {}
