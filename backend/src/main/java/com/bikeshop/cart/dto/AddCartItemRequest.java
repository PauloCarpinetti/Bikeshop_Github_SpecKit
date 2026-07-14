package com.bikeshop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull Long variacaoProdutoId,
        @Min(1) int quantidade
) {
}
