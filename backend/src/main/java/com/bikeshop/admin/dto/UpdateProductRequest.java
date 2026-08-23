package com.bikeshop.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record UpdateProductRequest(
        @NotBlank String nome,
        String descricao,
        @NotBlank String categoria,
        String marca,
        String modalidade,
        Map<String, Object> especificacoesTecnicas,
        Map<String, Object> tabelaGeometria,
        List<String> imagens
) {
}
