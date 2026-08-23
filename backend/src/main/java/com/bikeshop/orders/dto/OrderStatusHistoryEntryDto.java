package com.bikeshop.orders.dto;

import java.time.Instant;

public record OrderStatusHistoryEntryDto(String status, Instant timestamp) {
}
