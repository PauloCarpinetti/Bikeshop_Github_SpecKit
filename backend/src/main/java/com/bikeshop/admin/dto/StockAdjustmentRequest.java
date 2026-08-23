package com.bikeshop.admin.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(@NotNull Integer ajuste, String motivo) {
}
