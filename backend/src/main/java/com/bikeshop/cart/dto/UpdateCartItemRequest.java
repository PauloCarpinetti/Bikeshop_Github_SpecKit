package com.bikeshop.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(0) int quantidade
) {
}
