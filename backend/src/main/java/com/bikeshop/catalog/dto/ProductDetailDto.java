package com.bikeshop.catalog.dto;

import java.util.List;
import java.util.Map;

public record ProductDetailDto(
        Long id,
        String slug,
        String nome,
        String descricao,
        String categoria,
        String marca,
        String modalidade,
        Map<String, Object> especificacoesTecnicas,
        Map<String, Object> tabelaGeometria,
        List<String> imagens,
        String status,
        List<VariantDto> variacoes
) {
}
