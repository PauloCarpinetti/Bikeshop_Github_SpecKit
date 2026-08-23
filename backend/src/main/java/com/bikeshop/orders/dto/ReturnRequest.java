package com.bikeshop.orders.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnRequest(@NotBlank String motivo) {
}
