package com.bikeshop.customers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank String nome,
        String telefone,
        @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres") String novaSenha
) {
}
