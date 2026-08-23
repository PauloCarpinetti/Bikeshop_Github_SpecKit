package com.bikeshop.customers.dto;

import jakarta.validation.constraints.NotBlank;

public record EnderecoRequest(
        @NotBlank String cep,
        @NotBlank String logradouro,
        @NotBlank String numero,
        String complemento,
        @NotBlank String bairro,
        @NotBlank String cidade,
        @NotBlank String estado,
        String tipo,
        boolean padrao
) {
}
