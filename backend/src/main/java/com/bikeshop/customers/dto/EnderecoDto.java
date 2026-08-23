package com.bikeshop.customers.dto;

public record EnderecoDto(
        Long id,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String tipo,
        boolean padrao
) {
}
