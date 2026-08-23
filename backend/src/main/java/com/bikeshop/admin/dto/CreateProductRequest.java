package com.bikeshop.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record CreateProductRequest(
        @NotBlank String nome,
        String descricao,
        @NotBlank String categoria,
        String marca,
        String modalidade,
        Map<String, Object> especificacoesTecnicas,
        Map<String, Object> tabelaGeometria,
        List<String> imagens,
        @NotEmpty @Valid List<CreateVariantRequest> variantes
) {
}
