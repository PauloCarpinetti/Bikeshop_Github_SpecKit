package com.bikeshop.checkout.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponRequest(@NotBlank String codigo) {
}
