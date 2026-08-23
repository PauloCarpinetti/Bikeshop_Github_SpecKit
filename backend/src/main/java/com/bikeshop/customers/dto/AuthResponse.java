package com.bikeshop.customers.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long clienteId,
        String nome,
        String email,
        String role
) {
}
