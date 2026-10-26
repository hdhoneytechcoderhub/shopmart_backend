package com.shopmart.module.order.dto;

import java.time.Instant;

public record StatusHistoryResponse(String status, String note, Instant at) {}
