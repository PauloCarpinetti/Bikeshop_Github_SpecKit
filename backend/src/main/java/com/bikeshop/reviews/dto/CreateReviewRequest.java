package com.bikeshop.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull Long pedidoId,
        @NotNull Long variacaoProdutoId,
        @Min(1) @Max(5) int nota,
        String comentario
) {
}
