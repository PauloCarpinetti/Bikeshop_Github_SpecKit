package com.bikeshop.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCustomerStatusRequest(@NotNull Boolean bloqueado) {
}
