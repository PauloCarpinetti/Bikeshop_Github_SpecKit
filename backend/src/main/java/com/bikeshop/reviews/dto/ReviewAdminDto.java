package com.bikeshop.reviews.dto;

import java.time.Instant;

public record ReviewAdminDto(Long id, Long produtoId, Long clienteId, Long pedidoId, int nota, String comentario,
                              String status, Instant criadoEm) {
}
