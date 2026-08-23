package com.bikeshop.orders;

import jakarta.validation.constraints.NotBlank;

public record EnderecoEntregaInput(
        @NotBlank String cep,
        @NotBlank String logradouro,
        @NotBlank String numero,
        String complemento,
        @NotBlank String bairro,
        @NotBlank String cidade,
        @NotBlank String estado
) {
}
