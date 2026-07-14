package com.bikeshop.catalog.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VariantDto(
        Long id,
        String sku,
        Map<String, Object> atributos,
        BigDecimal preco,
        int estoqueDisponivel,
        String status
) {
}
