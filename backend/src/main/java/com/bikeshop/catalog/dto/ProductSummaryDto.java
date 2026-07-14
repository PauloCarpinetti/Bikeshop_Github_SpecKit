package com.bikeshop.catalog.dto;

import java.math.BigDecimal;

public record ProductSummaryDto(
        Long id,
        String slug,
        String nome,
        String categoria,
        String marca,
        String modalidade,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        String imagemPrincipal
) {
}
