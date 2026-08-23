package com.bikeshop.reviews.dto;

import java.time.Instant;

public record ReviewDto(
        Long id, Long produtoId, Long pedidoId, int nota, String comentario, String status, Instant criadoEm
) {
}
